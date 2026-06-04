package test

import models.MoveResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.BattleService
import service.BoardFactory
import service.BoardValidator

class BattleServiceTest {
    private lateinit var battleService: BattleService
    private lateinit var validator: BoardValidator
    private lateinit var factory: BoardFactory
    private lateinit var board: Array<Array<Char>>

    @BeforeEach
    fun setUp() {
        validator = BoardValidator()
        factory = BoardFactory()  // ← УБРАЛИ validator
        battleService = BattleService(validator)
        board = factory.createEmptyBoard()
    }

    @Test
    fun `should register hit correctly`() {
        validator.placeShip(board, 0, 0, 4, "right")
        val result = battleService.makeMove(board, 0, 0)
        assertEquals(MoveResult.HIT, result)
        assertEquals('X', board[0][0])
    }

    @Test
    fun `should register hit on vertical ship`() {
        validator.placeShip(board, 0, 5, 3, "down")
        val result = battleService.makeMove(board, 1, 5)
        assertEquals(MoveResult.HIT, result)
        assertEquals('X', board[1][5])
    }

    @Test
    fun `should detect kill for two-cell ship`() {
        validator.placeShip(board, 0, 0, 2, "right")
        battleService.makeMove(board, 0, 0)
        val result = battleService.makeMove(board, 0, 1)
        assertEquals(MoveResult.KILL, result)
        assertEquals('X', board[0][1])
    }

    @Test
    fun `should detect kill for three-cell vertical ship`() {
        validator.placeShip(board, 2, 2, 3, "down")
        battleService.makeMove(board, 2, 2)
        battleService.makeMove(board, 3, 2)
        val result = battleService.makeMove(board, 4, 2)
        assertEquals(MoveResult.KILL, result)
        assertEquals('X', board[4][2])
    }

    @Test
    fun `should detect kill for single-cell ship`() {
        validator.placeShip(board, 5, 5, 1, "right")
        val result = battleService.makeMove(board, 5, 5)
        assertEquals(MoveResult.KILL, result)
        assertEquals('X', board[5][5])
    }

    @Test
    fun `should register miss on empty cell`() {
        val result = battleService.makeMove(board, 5, 5)
        assertEquals(MoveResult.MISS, result)
        assertEquals('•', board[5][5])
    }

    @Test
    fun `should detect already shot cell after hit`() {
        validator.placeShip(board, 0, 0, 1, "right")
        battleService.makeMove(board, 0, 0)
        val result = battleService.makeMove(board, 0, 0)
        assertEquals(MoveResult.ALREADY_SHOT, result)
    }

    @Test
    fun `should detect already shot cell after miss`() {
        battleService.makeMove(board, 5, 5)
        val result = battleService.makeMove(board, 5, 5)
        assertEquals(MoveResult.ALREADY_SHOT, result)
        assertEquals('•', board[5][5])
    }

    @Test
    fun `should return invalid for out of bounds`() {
        val result = battleService.makeMove(board, -1, 0)
        assertEquals(MoveResult.INVALID, result)

        val result2 = battleService.makeMove(board, 10, 5)
        assertEquals(MoveResult.INVALID, result2)

        val result3 = battleService.makeMove(board, 5, -1)
        assertEquals(MoveResult.INVALID, result3)

        val result4 = battleService.makeMove(board, 5, 10)
        assertEquals(MoveResult.INVALID, result4)
    }

    @Test
    fun `should detect game over when all ships sunk`() {
        validator.placeShip(board, 0, 0, 1, "right")
        validator.placeShip(board, 1, 1, 1, "right")

        assertFalse(battleService.isGameOver(board))

        battleService.makeMove(board, 0, 0)
        assertFalse(battleService.isGameOver(board))

        battleService.makeMove(board, 1, 1)
        assertTrue(battleService.isGameOver(board))
    }

    @Test
    fun `should detect game over is false with remaining ships`() {
        validator.placeShip(board, 0, 0, 2, "right")
        validator.placeShip(board, 2, 2, 1, "right")

        battleService.makeMove(board, 0, 0)
        assertFalse(battleService.isGameOver(board))
    }

    @Test
    fun `should count remaining ships correctly`() {
        assertEquals(0, battleService.getRemainingShipsCount(board))

        validator.placeShip(board, 0, 0, 2, "right")
        assertEquals(2, battleService.getRemainingShipsCount(board))

        validator.placeShip(board, 2, 2, 3, "down")
        assertEquals(5, battleService.getRemainingShipsCount(board))

        battleService.makeMove(board, 0, 0)
        assertEquals(4, battleService.getRemainingShipsCount(board))

        battleService.makeMove(board, 0, 1)
        assertEquals(3, battleService.getRemainingShipsCount(board))
    }

    @Test
    fun `should count hits correctly`() {
        assertEquals(0, battleService.getHitCount(board))

        validator.placeShip(board, 0, 0, 3, "right")
        battleService.makeMove(board, 0, 0)
        assertEquals(1, battleService.getHitCount(board))

        battleService.makeMove(board, 0, 1)
        assertEquals(2, battleService.getHitCount(board))

        battleService.makeMove(board, 5, 5) // miss
        assertEquals(2, battleService.getHitCount(board))
    }

    @Test
    fun `should not count miss as hit`() {
        battleService.makeMove(board, 5, 5)
        assertEquals(0, battleService.getHitCount(board))
    }

    @Test
    fun `should handle multiple shots on same ship`() {
        validator.placeShip(board, 2, 2, 4, "right")

        val result1 = battleService.makeMove(board, 2, 2)
        assertEquals(MoveResult.HIT, result1)

        val result2 = battleService.makeMove(board, 2, 3)
        assertEquals(MoveResult.HIT, result2)

        val result3 = battleService.makeMove(board, 2, 4)
        assertEquals(MoveResult.HIT, result3)

        val result4 = battleService.makeMove(board, 2, 5)
        assertEquals(MoveResult.KILL, result4)

        assertEquals(4, battleService.getHitCount(board))
        assertTrue(battleService.isGameOver(board))
    }
}
