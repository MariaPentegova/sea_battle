package models

data class PlayerStatsRecord(
    val playerId: Int,
    val playerName: String = "",
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalHits: Int = 0,
    val totalMoves: Int = 0,
    val shipsSunk: Int = 0
) {
    val winRate: Float get() = if (gamesPlayed > 0) gamesWon.toFloat() / gamesPlayed else 0f
}
