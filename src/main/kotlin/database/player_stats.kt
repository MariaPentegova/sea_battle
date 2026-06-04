package database
import models.PlayerStatsRecord

class PlayerStatsRepository(private val dbManager: DatabaseManager) {

    fun refreshStatistics() {
        val conn = dbManager.getConnection()
        conn.prepareStatement("DELETE FROM player_statistics").execute()
        conn.prepareStatement("""
            INSERT INTO player_statistics (player_id, games_played, games_won, total_hits, total_moves, ships_sunk)
            SELECT 
                p.id,
                COUNT(DISTINCT g.id) as games_played,
                SUM(CASE WHEN g.winner_id = p.id THEN 1 ELSE 0 END) as games_won,
                COALESCE(SUM(CASE WHEN m.is_hit = 1 THEN 1 ELSE 0 END), 0) as total_hits,
                COALESCE(COUNT(m.id), 0) as total_moves,
                COALESCE(SUM(CASE WHEN m.is_kill = 1 THEN 1 ELSE 0 END), 0) as ships_sunk
            FROM players p
            LEFT JOIN games g ON g.player1_id = p.id OR g.player2_id = p.id
            LEFT JOIN moves m ON m.player_id = p.id
            GROUP BY p.id
        """.trimIndent()).execute()
    }

    fun getPlayerStats(playerId: Int): PlayerStatsRecord? {
        val conn = dbManager.getConnection()
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

    fun getAllPlayerStats(): List<PlayerStatsRecord> {
        val conn = dbManager.getConnection()
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
}
