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

    // ========== УСПЕШНАЯ РАССТАНОВКА ==========

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

    // ========== ВЫХОД ЗА ГРАНИЦЫ ==========

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

    // ========== ПЕРЕСЕЧЕНИЕ ==========

    @Test
    fun `should detect overlap when ships collide`() {
        validator.placeShip(board, 0, 0, 4, "right")
        val result = validator.canPlaceShip(board, 0, 2, 3, "right")
        assertEquals(ShipPlacementResult.OVERLAP, result)
    }

    // ========== ЗАЗОР МЕЖДУ КОРАБЛЯМИ ==========

    @Test
    fun `should detect ships too close horizontally`() {
        validator.placeShip(board, 0, 0, 2, "right")
        val result = validator.canPlaceShip(board, 0, 2, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)
    }

    @Test
    fun `should detect ships too close vertically`() {
        validator.placeShip(board, 0, 0, 2, "down")
        val result = validator.canPlaceShip(board, 2, 0, 2, "down")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)
    }

    @Test
    fun `should allow ships with proper gap`() {
        validator.placeShip(board, 0, 0, 2, "right")
        val result = validator.canPlaceShip(board, 0, 3, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
    }

    // ========== ОДНОПАЛУБНЫЙ КОРАБЛЬ ==========

    @Test
    fun `should place single cell ship`() {
        val result = validator.canPlaceShip(board, 5, 5, 1, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
        validator.placeShip(board, 5, 5, 1, "right")
        assertEquals('■', board[5][5])
    }

    // ========== НЕВЕРНОЕ НАПРАВЛЕНИЕ ==========

    @Test
    fun `should validate invalid direction`() {
        val result = validator.canPlaceShip(board, 0, 0, 3, "diagonal")
        assertEquals(ShipPlacementResult.INVALID_DIRECTION, result)
    }
}
