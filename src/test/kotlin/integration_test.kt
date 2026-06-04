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
    fun `integration - full game creation and ship placement on both boards`() {
        // Проверяет связку: GameManager + BoardValidator + BoardFactory
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        val result1 = gameManager.placeShip(p1.id, 0, 0, 3, "right")
        val result2 = gameManager.placeShip(p2.id, 5, 5, 2, "down")

        assertEquals(ShipPlacementResult.SUCCESS, result1)
        assertEquals(ShipPlacementResult.SUCCESS, result2)

        val game = gameManager.getCurrentGame()
        assertNotNull(game)
        assertEquals('■', game?.board1?.get(0)?.get(0))
        assertEquals('■', game?.board1?.get(0)?.get(2))
        assertEquals('■', game?.board2?.get(5)?.get(5))
        assertEquals('■', game?.board2?.get(6)?.get(5))
    }

    @Test
    fun `integration - battle flow with hit, miss and turn switch`() {
        // Проверяет связку: GameManager + BattleService + BoardValidator
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем корабли
        gameManager.placeShip(p2.id, 0, 0, 2, "right")  // клетки (0,0) и (0,1)
        gameManager.placeShip(p1.id, 5, 5, 1, "right")

        // Ход p1: попадание (не потопил)
        val hitResult = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, hitResult)
        
        // Ход должен остаться у p1
        var game = gameManager.getCurrentGame()
        assertEquals(p1.id, game?.currentPlayer?.id)

        // Ход p1: промах
        val missResult = gameManager.makeMove(p1.id, 9, 9)
        assertEquals(MoveResult.MISS, missResult)
        
        // Ход должен перейти к p2
        game = gameManager.getCurrentGame()
        assertEquals(p2.id, game?.currentPlayer?.id)
    }

    @Test
    fun `integration - complete victory with kill detection and game end`() {
        // Проверяет связку: GameManager + BattleService + определение победителя
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Только один однопалубный корабль у p2
        gameManager.placeShip(p2.id, 3, 3, 1, "right")

        val result = gameManager.makeMove(p1.id, 3, 3)
        assertEquals(MoveResult.KILL, result)

        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
        assertEquals(p1.id, game?.winner?.id)
        assertTrue(gameManager.getGameStats().player2Ships == 0)
    }
}
