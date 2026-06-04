package database

import java.sql.Types
import models.GameMove
import models.StoredGame

class GameHistoryRepository(private val dbManager: DatabaseManager) {

    fun saveGame(game: StoredGame) {
        val conn = dbManager.getConnection()

        // Сохраняем игру
        val stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO games (id, player1_id, player2_id, winner_id, start_time, end_time)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent())
        stmt.setString(1, game.id)
        stmt.setInt(2, game.player1Id)
        stmt.setInt(3, game.player2Id)
        if (game.winnerId != null) {
            stmt.setInt(4, game.winnerId)
        } else {
            stmt.setNull(4, Types.INTEGER)
        }
        stmt.setLong(5, game.startTime)
        if (game.endTime != null) {
            stmt.setLong(6, game.endTime)
        } else {
            stmt.setNull(6, Types.BIGINT)
        }
        stmt.execute()

        // Удаляем старые ходы (если игра уже была сохранена)
        val deleteStmt = conn.prepareStatement("DELETE FROM moves WHERE game_id = ?")
        deleteStmt.setString(1, game.id)
        deleteStmt.execute()

        // Сохраняем ходы
        for (move in game.moves) {
            val moveStmt = conn.prepareStatement("""
                INSERT INTO moves (game_id, player_id, row, col, is_hit, is_kill, move_number, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent())
            moveStmt.setString(1, game.id)
            moveStmt.setInt(2, move.playerId)
            moveStmt.setInt(3, move.row)
            moveStmt.setInt(4, move.col)
            moveStmt.setInt(5, if (move.isHit) 1 else 0)
            moveStmt.setInt(6, if (move.isKill) 1 else 0)
            moveStmt.setInt(7, move.moveNumber)
            moveStmt.setLong(8, move.timestamp)
            moveStmt.execute()
        }
    }

    fun getAllGames(): List<StoredGame> {
        val conn = dbManager.getConnection()
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

    fun getGameById(gameId: String): StoredGame? {
        val conn = dbManager.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM games WHERE id = ?")
        stmt.setString(1, gameId)
        val rs = stmt.executeQuery()

        return if (rs.next()) {
            StoredGame(
                id = gameId,
                player1Id = rs.getInt("player1_id"),
                player2Id = rs.getInt("player2_id"),
                winnerId = rs.getInt("winner_id").takeIf { it > 0 },
                startTime = rs.getLong("start_time"),
                endTime = rs.getLong("end_time").takeIf { it > 0 },
                moves = getMovesForGame(gameId)
            )
        } else null
    }

    fun getGamesByPlayer(playerId: Int): List<StoredGame> {
        val conn = dbManager.getConnection()
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
        val conn = dbManager.getConnection()
        val moves = mutableListOf< GameMove>()

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
}
