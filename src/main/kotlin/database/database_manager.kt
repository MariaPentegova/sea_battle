package database

import models.GameMove
import models.StoredGame
import models.PlayerStatsRecord
import java.sql.Connection
import java.sql.DriverManager

class DatabaseManager(private val dbPath: String = "battleship.db") {

    private var connection: Connection? = null

    fun getConnection(): Connection {
        if (connection == null || connection?.isClosed == true) {
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        }
        return connection!!
    }

    fun initialize() {
        val conn = getConnection()

        // Таблица игроков
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS players (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL
            )
        """.trimIndent()).execute()

        // Таблица игр (партий)
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS games (
                id TEXT PRIMARY KEY,
                player1_id INTEGER NOT NULL,
                player2_id INTEGER NOT NULL,
                winner_id INTEGER,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                FOREIGN KEY (player1_id) REFERENCES players(id),
                FOREIGN KEY (player2_id) REFERENCES players(id),
                FOREIGN KEY (winner_id) REFERENCES players(id)
            )
        """.trimIndent()).execute()

        // Таблица ходов
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS moves (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                game_id TEXT NOT NULL,
                player_id INTEGER NOT NULL,
                row INTEGER NOT NULL,
                col INTEGER NOT NULL,
                is_hit INTEGER NOT NULL,
                is_kill INTEGER NOT NULL,
                move_number INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (game_id) REFERENCES games(id),
                FOREIGN KEY (player_id) REFERENCES players(id)
            )
        """.trimIndent()).execute()

        // Таблица статистики (материализованное представление)
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_statistics (
                player_id INTEGER PRIMARY KEY,
                games_played INTEGER DEFAULT 0,
                games_won INTEGER DEFAULT 0,
                total_hits INTEGER DEFAULT 0,
                total_moves INTEGER DEFAULT 0,
                ships_sunk INTEGER DEFAULT 0,
                FOREIGN KEY (player_id) REFERENCES players(id)
            )
        """.trimIndent()).execute()
    }

    fun savePlayer(id: Int, name: String) {
        val conn = getConnection()
        val stmt = conn.prepareStatement("INSERT OR REPLACE INTO players (id, name) VALUES (?, ?)")
        stmt.setInt(1, id)
        stmt.setString(2, name)
        stmt.execute()
    }

    fun getAllPlayers(): List<Pair<Int, String>> {
        val conn = getConnection()
        val rs = conn.prepareStatement("SELECT id, name FROM players ORDER BY id").executeQuery()
        val players = mutableListOf<Pair<Int, String>>()
        while (rs.next()) {
            players.add(rs.getInt("id") to rs.getString("name"))
        }
        return players
    }

    fun saveGame(
        gameId: String,
        player1Id: Int,
        player2Id: Int,
        winnerId: Int?,
        startTime: Long,
        endTime: Long?,
        moves: List<GameMove>
    ) {
        val conn = getConnection()

        // Сохраняем игру
        val stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO games (id, player1_id, player2_id, winner_id, start_time, end_time)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent())
        stmt.setString(1, gameId)
        stmt.setInt(2, player1Id)
        stmt.setInt(3, player2Id)
        stmt.setInt(4, winnerId ?: -1)
        stmt.setLong(5, startTime)
        stmt.setLong(6, endTime ?: 0)
        stmt.execute()

        // Удаляем старые ходы
        conn.prepareStatement("DELETE FROM moves WHERE game_id = ?").apply {
            setString(1, gameId)
            execute()
        }

        // Сохраняем ходы
        for (move in moves) {
            val moveStmt = conn.prepareStatement("""
                INSERT INTO moves (game_id, player_id, row, col, is_hit, is_kill, move_number, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent())
            moveStmt.setString(1, gameId)
            moveStmt.setInt(2, move.playerId)
            moveStmt.setInt(3, move.row)
            moveStmt.setInt(4, move.col)
            moveStmt.setInt(5, if (move.isHit) 1 else 0)
            moveStmt.setInt(6, if (move.isKill) 1 else 0)
            moveStmt.setInt(7, move.moveNumber)
            moveStmt.setLong(8, move.timestamp)
            moveStmt.execute()
        }

        refreshStatistics()
    }

    private fun refreshStatistics() {
        val conn = getConnection()

        conn.prepareStatement("DELETE FROM player_statistics").execute()

        conn.prepareStatement("""
        INSERT INTO player_statistics (player_id, games_played, games_won, total_hits, total_moves, ships_sunk)
        SELECT 
            p.id,
            COUNT(DISTINCT g.id) as games_played,
            SUM(CASE WHEN g.winner_id = p.id THEN 1 ELSE 0 END) as games_won,
            COALESCE(SUM(CASE WHEN m.is_hit = 1 AND m.player_id = p.id THEN 1 ELSE 0 END), 0) as total_hits,
            COALESCE(COUNT(CASE WHEN m.player_id = p.id THEN 1 END), 0) as total_moves,
            COALESCE(SUM(CASE WHEN m.is_kill = 1 AND m.player_id = p.id THEN 1 ELSE 0 END), 0) as ships_sunk
        FROM players p
        LEFT JOIN games g ON g.player1_id = p.id OR g.player2_id = p.id
        LEFT JOIN moves m ON m.game_id = g.id
        GROUP BY p.id
    """.trimIndent()).execute()
    }

    fun getAllGames(): List<StoredGame> {
        val conn = getConnection()
        val games = mutableListOf<StoredGame>()

        val rs = conn.prepareStatement("SELECT * FROM games ORDER BY start_time DESC").executeQuery()
        while (rs.next()) {
            val gameId = rs.getString("id")
            games.add(StoredGame(
                id = gameId,
                player1Id = rs.getInt("player1_id"),
                player2Id = rs.getInt("player2_id"),
                winnerId = rs.getInt("winner_id").takeIf { it > 0 },
                startTime = rs.getLong("start_time"),
                endTime = rs.getLong("end_time").takeIf { it > 0 },
                moves = getMovesForGame(gameId)
            ))
        }
        return games
    }

    fun getGamesByPlayer(playerId: Int): List<StoredGame> {
        val conn = getConnection()
        val games = mutableListOf<StoredGame>()

        val stmt = conn.prepareStatement("""
            SELECT * FROM games WHERE player1_id = ? OR player2_id = ? ORDER BY start_time DESC
        """.trimIndent())
        stmt.setInt(1, playerId)
        stmt.setInt(2, playerId)
        val rs = stmt.executeQuery()

        while (rs.next()) {
            val gameId = rs.getString("id")
            games.add(StoredGame(
                id = gameId,
                player1Id = rs.getInt("player1_id"),
                player2Id = rs.getInt("player2_id"),
                winnerId = rs.getInt("winner_id").takeIf { it > 0 },
                startTime = rs.getLong("start_time"),
                endTime = rs.getLong("end_time").takeIf { it > 0 },
                moves = getMovesForGame(gameId)
            ))
        }
        return games
    }

    private fun getMovesForGame(gameId: String): List<GameMove> {
        val conn = getConnection()
        val moves = mutableListOf<GameMove>()

        val stmt = conn.prepareStatement("SELECT * FROM moves WHERE game_id = ? ORDER BY move_number")
        stmt.setString(1, gameId)
        val rs = stmt.executeQuery()

        while (rs.next()) {
            moves.add(GameMove(
                playerId = rs.getInt("player_id"),
                row = rs.getInt("row"),
                col = rs.getInt("col"),
                isHit = rs.getInt("is_hit") == 1,
                isKill = rs.getInt("is_kill") == 1,
                moveNumber = rs.getInt("move_number"),
                timestamp = rs.getLong("timestamp")
            ))
        }
        return moves
    }

    fun getAllPlayerStatistics(): List<PlayerStatsRecord> {
        val conn = getConnection()
        val stats = mutableListOf<PlayerStatsRecord>()

        val rs = conn.prepareStatement("""
            SELECT ps.*, p.name 
            FROM player_statistics ps
            JOIN players p ON ps.player_id = p.id
            ORDER BY ps.games_won DESC, ps.games_played DESC
        """.trimIndent()).executeQuery()

        while (rs.next()) {
            stats.add(PlayerStatsRecord(
                playerId = rs.getInt("player_id"),
                playerName = rs.getString("name"),
                gamesPlayed = rs.getInt("games_played"),
                gamesWon = rs.getInt("games_won"),
                totalHits = rs.getInt("total_hits"),
                totalMoves = rs.getInt("total_moves"),
                shipsSunk = rs.getInt("ships_sunk")
            ))
        }
        return stats
    }

    fun getPlayerStatistics(playerId: Int): PlayerStatsRecord? {
        val conn = getConnection()
        val stmt = conn.prepareStatement("""
            SELECT ps.*, p.name 
            FROM player_statistics ps
            JOIN players p ON ps.player_id = p.id
            WHERE ps.player_id = ?
        """.trimIndent())
        stmt.setInt(1, playerId)
        val rs = stmt.executeQuery()

        return if (rs.next()) {
            PlayerStatsRecord(
                playerId = rs.getInt("player_id"),
                playerName = rs.getString("name"),
                gamesPlayed = rs.getInt("games_played"),
                gamesWon = rs.getInt("games_won"),
                totalHits = rs.getInt("total_hits"),
                totalMoves = rs.getInt("total_moves"),
                shipsSunk = rs.getInt("ships_sunk")
            )
        } else null
    }

    fun clearAll() {
        val conn = getConnection()
        conn.prepareStatement("DELETE FROM moves").execute()
        conn.prepareStatement("DELETE FROM games").execute()
        conn.prepareStatement("DELETE FROM player_statistics").execute()
        conn.prepareStatement("DELETE FROM players").execute()
    }
}
