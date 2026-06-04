package models

enum class ShipPlacementResult {
    SUCCESS,
    OUT_OF_BOUNDS,
    OVERLAP,
    TOO_CLOSE,
    INVALID_DIRECTION
}
