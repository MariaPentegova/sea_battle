package models

data class GameMove(
    val playerId: Int,
    val row: Int,
    val col: Int,
    val isHit: Boolean,
    val isKill: Boolean,
    val moveNumber: Int,
    val timestamp: Long
)

enum class MoveResult {
    HIT,
    MISS,
    KILL,
    GAME_WON,
    INVALID,
    ALREADY_SHOT
}
