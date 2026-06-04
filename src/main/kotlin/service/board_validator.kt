package service

import models.ShipPlacementResult

class BoardValidator {
    companion object {
        const val SIZE = 10
    }

    fun canPlaceShip(
        board: Array<Array<Char>>,
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): ShipPlacementResult {
        val validDirections = setOf("up", "down", "left", "right")
        if (direction.lowercase() !in validDirections) {
            return ShipPlacementResult.INVALID_DIRECTION
        }

        val cells = getShipCells(startRow, startCol, length, direction)

        // Проверка границ
        if (cells.isEmpty()) return ShipPlacementResult.OUT_OF_BOUNDS

        // Проверка занятости клеток
        for ((r, c) in cells) {
            if (board[r][c] != '~') return ShipPlacementResult.OVERLAP
        }

        // Проверка всех соседей для всех клеток корабля
        for ((r, c) in cells) {
            // Проверяем все клетки вокруг (включая диагонали)
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (isInBounds(nr, nc)) {
                        // Если соседняя клетка не является частью нашего нового корабля
                        // и в ней есть корабль, то нельзя ставить
                        if ((nr to nc) !in cells && board[nr][nc] != '~') {
                            return ShipPlacementResult.TOO_CLOSE
                        }
                    }
                }
            }
        }

        return ShipPlacementResult.SUCCESS
    }

    fun placeShip(
        board: Array<Array<Char>>,
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): Boolean {
        val cells = getShipCells(startRow, startCol, length, direction)
        for ((r, c) in cells) {
            board[r][c] = '■'
        }
        return true
    }

    fun isInBounds(row: Int, col: Int): Boolean = row in 0 until SIZE && col in 0 until SIZE

    private fun getShipCells(
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        repeat(length) { step ->
            val (r, c) = when (direction.lowercase()) {
                "up" -> startRow - step to startCol
                "down" -> startRow + step to startCol
                "left" -> startRow to startCol - step
                "right" -> startRow to startCol + step
                else -> return emptyList()
            }
            if (!isInBounds(r, c)) return emptyList()
            cells.add(r to c)
        }
        return cells
    }
}
