package test

import models.MoveResult
import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.*

class IntegrationTest {
    private lateinit var validator: BoardValidator
    private lateinit var battleService: BattleService
    private lateinit var factory: BoardFactory
    private lateinit var gameManager: GameManager

    @BeforeEach
    fun setUp() {
        validator = BoardValidator()
        battleService = BattleService(validator)
        factory = BoardFactory()
        gameManager = GameManager(validator, battleService, factory)
    }

    @Test
    fun `integration - full game creation and ship placement`() {
        // Проверяет: GameManager + BoardValidator + BoardFactory
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)

        val result1 = gameManager.placeShip(p1.id, 0, 0, 3, "right")
        val result2 = gameManager.placeShip(p2.id, 5, 5, 2, "down")
        assertEquals(ShipPlacementResult.SUCCESS, result1)
        assertEquals(ShipPlacementResult.SUCCESS, result2)

        // Проверяем, что корабли действительно на досках
        val gameState = gameManager.getCurrentGame()
        assertEquals('■', gameState?.board1?.get(0)?.get(0))
        assertEquals('■', gameState?.board1?.get(0)?.get(2))
        assertEquals('■', gameState?.board2?.get(5)?.get(5))
        assertEquals('■', gameState?.board2?.get(6)?.get(5))
    }

    @Test
    fun `integration - battle with hit, miss and turn switching`() {
        // Проверяет: GameManager + BattleService + логику смены хода
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p2.id, 0, 0, 2, "right")  // клетки (0,0) и (0,1)
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        // Попадание — ход остаётся у p1
        val hitResult = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, hitResult)
        var game = gameManager.getCurrentGame()
        assertEquals(p1.id, game?.currentPlayer?.id)

        // Промах — ход переходит к p2
        val missResult = gameManager.makeMove(p1.id, 9, 9)
        assertEquals(MoveResult.MISS, missResult)
        game = gameManager.getCurrentGame()
        assertEquals(p2.id, game?.currentPlayer?.id)
    }

    @Test
    fun `integration - kill detection and game victory`() {
        // Проверяет: BattleService.checkKill + GameManager определение победителя
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Только один однопалубный корабль у p2
        gameManager.placeShip(p2.id, 3, 3, 1, "right")
        gameManager.placeShip(p1.id, 0, 0, 1, "right")  // чтобы не было null

        val result = gameManager.makeMove(p1.id, 3, 3)
        assertEquals(MoveResult.KILL, result)

        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
        assertEquals(p1.id, game?.winner?.id)
        assertEquals(0, gameManager.getGameStats().player2Ships)
    }

    @Test
    fun `integration - ship placement constraints`() {
        // Проверяет: BoardValidator.canPlaceShip с разными сценариями
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Успешная расстановка
        var result = gameManager.placeShip(p1.id, 0, 0, 3, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)

        // Слишком близко (TOO_CLOSE)
        result = gameManager.placeShip(p1.id, 0, 3, 2, "right")
        assertEquals(ShipPlacementResult.TOO_CLOSE, result)

        // Пересечение (OVERLAP)
        result = gameManager.placeShip(p1.id, 0, 1, 2, "right")
        assertEquals(ShipPlacementResult.OVERLAP, result)

        // Выход за границы
        result = gameManager.placeShip(p1.id, 9, 8, 3, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }
}
