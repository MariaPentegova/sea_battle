package models

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (player1 != other.player1) return false
        if (player2 != other.player2) return false
        if (!board1.contentDeepEquals(other.board1)) return false
        if (!board2.contentDeepEquals(other.board2)) return false
        if (currentPlayer != other.currentPlayer) return false
        if (winner != other.winner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = player1.hashCode()
        result = 31 * result + player2.hashCode()
        result = 31 * result + board1.contentDeepHashCode()
        result = 31 * result + board2.contentDeepHashCode()
        result = 31 * result + currentPlayer.hashCode()
        result = 31 * result + (winner?.hashCode() ?: 0)
        return result
    }
}
