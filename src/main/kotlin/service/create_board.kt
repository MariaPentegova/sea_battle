package service

class BoardFactory {

    fun createEmptyBoard(): Array<Array<Char>> {
        return Array(BoardValidator.SIZE) { Array(BoardValidator.SIZE) { '~' } }
    }

    fun copyBoard(source: Array<Array<Char>>): Array<Array<Char>> {
        val copy = createEmptyBoard()
        for (i in source.indices) {
            for (j in source[i].indices) {
                copy[i][j] = source[i][j]
            }
        }
        return copy
    }

    fun getHiddenBoard(original: Array<Array<Char>>): Array<Array<Char>> {
        val hidden = createEmptyBoard()
        for (i in original.indices) {
            for (j in original[i].indices) {
                hidden[i][j] = when (original[i][j]) {
                    '■' -> '~'
                    else -> original[i][j]
                }
            }
        }
        return hidden
    }

    fun boardsAreEqual(board1: Array<Array<Char>>, board2: Array<Array<Char>>): Boolean {
        for (i in board1.indices) {
            for (j in board1[i].indices) {
                if (board1[i][j] != board2[i][j]) return false
            }
        }
        return true
    }
}
