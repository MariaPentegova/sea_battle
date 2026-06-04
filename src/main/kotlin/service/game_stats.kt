package service

data class GameStats(
    val player1Id: Int,
    val player1Name: String,
    val player1Ships: Int,
    val player1Hits: Int,
    val player2Id: Int,
    val player2Name: String,
    val player2Ships: Int,
    val player2Hits: Int,
    val currentPlayerId: Int,
    val currentPlayerName: String
) {
    companion object {
        fun empty() = GameStats(0, "", 0, 0, 0, "", 0, 0, 0, "")
    }
}
