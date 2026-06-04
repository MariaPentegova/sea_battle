package test

import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.BoardFactory
import service.BoardValidator

class BoardValidatorTest {
    private lateinit var validator: BoardValidator
    private lateinit var factory: BoardFactory
    private lateinit var board: Array<Array<Char>>

    @BeforeEach
    fun setUp() {
        validator = BoardValidator()
        factory = BoardFactory()
        board = factory.createEmptyBoard()
    }

    @Test
    fun `should place horizontal ship successfully`() {
        val result = validator.canPlaceShip(board, 0, 0, 4, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
        validator.placeShip(board, 0, 0, 4, "right")
        assertEquals('■', board[0][0])
        assertEquals('■', board[0][3])
    }

    @Test
    fun `should place vertical ship successfully`() {
        val result = validator.canPlaceShip(board, 0, 5, 3, "down")
        assertEquals(ShipPlacementResult.SUCCESS, result)
        validator.placeShip(board, 0, 5, 3, "down")
        assertEquals('■', board[0][5])
        assertEquals('■', board[2][5])
    }

    @Test
    fun `should detect out of bounds right`() {
        val result = validator.canPlaceShip(board, 0, 8, 3, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }

    @Test
    fun `should detect out of bounds down`() {
        val result = validator.canPlaceShip(board, 8, 0, 3, "down")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }

    @Test
    fun `should detect out of bounds left`() {
        val result = validator.canPlaceShip(board, 0, 1, 3, "left")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }

    @Test
    fun `should detect out of bounds up`() {
        val result = validator.canPlaceShip(board, 1, 0, 3, "up")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }

    @Test
    fun `should detect overlap when ships collide`() {
        validator.placeShip(board, 0, 0, 4, "right")
        val result = validator.canPlaceShip(board, 0, 2, 3, "right")
        assertEquals(ShipPlacementResult.OVERLAP, result)
    }

    @Test
    fun `should detect ships too close horizontally`() {
        // Ставим корабль на клетки (0,0) и (0,1)
        validator.placeShip(board, 0, 0, 2, "right")

        // Пытаемся поставить корабль на клетки (0,3) и (0,4)
        // Клетка (0,2) находится между ними - это соседняя клетка для (0,1)
        // И она пустая, но проблема в том, что (0,3) соседствует с (0,2)?
        // Нет, (0,3) соседствует с (0,2), но (0,2) пустая. А сам (0,3) не соседствует с (0,1) напрямую?
        // (0,1) и (0,3) не соседи (между ними (0,2))
        // Значит TOO_CLOSE не сработает! Нужно ставить на (0,2) и (0,3)

        // Очищаем доску
        board = factory.createEmptyBoard()

        // Ставим корабль на (0,0)-(0,1)
        validator.placeShip(board, 0, 0, 2, "right")

        // Пытаемся поставить корабль на (0,2)-(0,3) - прямо вплотную
        val result = validator.canPlaceShip(board, 0, 2, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)
    }

    @Test
    fun `should detect ships too close vertically`() {
        // Ставим корабль на (0,0)-(1,0)
        validator.placeShip(board, 0, 0, 2, "down")

        // Пытаемся поставить корабль на (2,0)-(3,0) - прямо вплотную
        val result = validator.canPlaceShip(board, 2, 0, 2, "down")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)
    }

    @Test
    fun `should detect ships too close diagonally`() {
        // Ставим корабль на (0,0)
        validator.placeShip(board, 0, 0, 1, "right")

        // Пытаемся поставить корабль на (1,1) - по диагонали
        val result = validator.canPlaceShip(board, 1, 1, 1, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)
    }

    @Test
    fun `should allow ships with proper gap horizontally`() {
        // Ставим корабль на (0,0)-(0,1)
        validator.placeShip(board, 0, 0, 2, "right")

        // Пытаемся поставить корабль на (0,4)-(0,5)
        // Между (0,1) и (0,4) есть клетки (0,2) и (0,3) - зазор 2 клетки
        val result = validator.canPlaceShip(board, 0, 4, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
    }

    @Test
    fun `should allow ships with proper gap vertically`() {
        // Ставим корабль на (0,0)-(1,0)
        validator.placeShip(board, 0, 0, 2, "down")

        // Пытаемся поставить корабль на (4,0)-(5,0)
        // Между (1,0) и (4,0) есть клетки (2,0) и (3,0) - зазор 2 клетки
        val result = validator.canPlaceShip(board, 4, 0, 2, "down")
        assertEquals(ShipPlacementResult.SUCCESS, result)
    }

    @Test
    fun `should place single cell ship`() {
        val result = validator.canPlaceShip(board, 5, 5, 1, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
        validator.placeShip(board, 5, 5, 1, "right")
        assertEquals('■', board[5][5])
    }

    @Test
    fun `should validate invalid direction`() {
        val result = validator.canPlaceShip(board, 0, 0, 3, "diagonal")
        assertEquals(ShipPlacementResult.INVALID_DIRECTION, result)
    }
}
