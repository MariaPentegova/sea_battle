package service

import models.MoveResult

class BattleService(private val validator: BoardValidator) {

    fun makeMove(board: Array<Array<Char>>, row: Int, col: Int): MoveResult {
        if (!validator.isInBounds(row, col)) return MoveResult.INVALID

        return when (board[row][col]) {
            '■' -> {
                board[row][col] = 'X'
                if (checkKill(board, row, col)) MoveResult.KILL else MoveResult.HIT
            }
            '~' -> {
                board[row][col] = '•'
                MoveResult.MISS
            }
            'X', '•' -> MoveResult.ALREADY_SHOT
            else -> MoveResult.INVALID
        }
    }

    fun isGameOver(board: Array<Array<Char>>): Boolean {
        for (row in board) {
            for (cell in row) {
                if (cell == '■') return false
            }
        }
        return true
    }

    fun getRemainingShipsCount(board: Array<Array<Char>>): Int {
        var count = 0
        for (row in board) {
            for (cell in row) {
                if (cell == '■') count++
            }
        }
        return count
    }

    fun getHitCount(board: Array<Array<Char>>): Int {
        var count = 0
        for (row in board) {
            for (cell in row) {
                if (cell == 'X') {
                    count++
                }
            }
        }
        return count
    }

    private fun checkKill(board: Array<Array<Char>>, hitRow: Int, hitCol: Int): Boolean {
        val shipCells = mutableSetOf<Pair<Int, Int>>()
        val queue = mutableListOf(hitRow to hitCol)

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeAt(0)
            if ((r to c) in shipCells) continue
            if (board[r][c] == '■' || board[r][c] == 'X') {
                shipCells.add(r to c)
                val neighbors = listOf(r to c + 1, r to c - 1, r + 1 to c, r - 1 to c)
                for ((nr, nc) in neighbors) {
                    if (validator.isInBounds(nr, nc) && (board[nr][nc] == '■' || board[nr][nc] == 'X')) {
                        queue.add(nr to nc)
                    }
                }
            }
        }

        for ((r, c) in shipCells) {
            if (board[r][c] != 'X') return false
        }
        return true
    }
}
