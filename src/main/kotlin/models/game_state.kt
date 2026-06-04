package models

//показ статистики
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

//текущее состояние игры
data class GameState(
    val player1: Player,
    val player2: Player,
    val board1: Array<Array<Char>>,
    val board2: Array<Array<Char>>,
    var currentPlayer: Player,
    var winner: Player? = null
) {
    fun getBoard(player: Player): Array<Array<Char>> =
        if (player == player1) board1 else board2

    fun getOpponent(player: Player): Player =
        if (player == player1) player2 else player1

    fun switchTurn() {
        currentPlayer = getOpponent(currentPlayer)
    }
}
