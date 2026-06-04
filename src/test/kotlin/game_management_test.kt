package test

import models.MoveResult
import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.*

class GameManagerTest {
    private lateinit var gameManager: GameManager
    private lateinit var validator: BoardValidator
    private lateinit var battleService: BattleService
    private lateinit var factory: BoardFactory

    @BeforeEach
    fun setUp() {
        validator = BoardValidator()
        battleService = BattleService(validator)
        factory = BoardFactory()
        gameManager = GameManager(validator, battleService, factory)
    }

    @Test
    fun `should add player successfully`() {
        val player = gameManager.addPlayer("Анна")
        assertEquals(1, player.id)
        assertEquals("Анна", player.name)
        assertEquals(1, gameManager.getAllPlayers().size)
    }

    @Test
    fun `should add multiple players with different IDs`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        val p3 = gameManager.addPlayer("Светлана")

        assertEquals(1, p1.id)
        assertEquals(2, p2.id)
        assertEquals(3, p3.id)
        assertEquals(3, gameManager.getAllPlayers().size)
    }

    @Test
    fun `should add players with same name but different IDs`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Анна")

        assertNotEquals(p1.id, p2.id)
        assertEquals("Анна", p1.name)
        assertEquals("Анна", p2.name)
        assertEquals(2, gameManager.getAllPlayers().size)
    }

    @Test
    fun `should trim player names`() {
        val player = gameManager.addPlayer("  Анна  ")
        assertEquals("Анна", player.name)
    }

    @Test
    fun `should retrieve player by ID`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        assertEquals(p1, gameManager.getPlayerById(1))
        assertEquals(p2, gameManager.getPlayerById(2))
        assertNull(gameManager.getPlayerById(999))
    }

    @Test
    fun `should get all players list`() {
        gameManager.addPlayer("Анна")
        gameManager.addPlayer("Борис")
        gameManager.addPlayer("Светлана")

        val players = gameManager.getAllPlayers()
        assertEquals(3, players.size)
        assertEquals("Анна", players[0].name)
        assertEquals("Борис", players[1].name)
        assertEquals("Светлана", players[2].name)
    }

    @Test
    fun `should create game successfully`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        val game = gameManager.createGame(p1.id, p2.id)

        assertNotNull(game)
        assertEquals(p1.id, game?.player1?.id)
        assertEquals(p2.id, game?.player2?.id)
        assertEquals(p1.id, game?.currentPlayer?.id)
        assertNull(game?.winner)
        assertNotNull(game?.board1)
        assertNotNull(game?.board2)
    }

    @Test
    fun `should not create game with same player`() {
        val p1 = gameManager.addPlayer("Анна")
        val game = gameManager.createGame(p1.id, p1.id)
        assertNull(game)
    }

    @Test
    fun `should not create game with non-existent player`() {
        gameManager.addPlayer("Анна")
        val game = gameManager.createGame(1, 999)
        assertNull(game)
    }

    @Test
    fun `should get current game after creation`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        assertNull(gameManager.getCurrentGame())

        gameManager.createGame(p1.id, p2.id)
        assertNotNull(gameManager.getCurrentGame())
    }

    @Test
    fun `should finish game correctly`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        gameManager.createGame(p1.id, p2.id)
        assertNotNull(gameManager.getCurrentGame())

        gameManager.finishGame()
        assertNull(gameManager.getCurrentGame())
    }

    @Test
    fun `should place ship successfully in game`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        val result = gameManager.placeShip(p1.id, 0, 0, 4, "right")
        assertEquals(ShipPlacementResult.SUCCESS, result)
    }

    @Test
    fun `should not place ship outside game`() {
        val p1 = gameManager.addPlayer("Анна")
        val result = gameManager.placeShip(p1.id, 0, 0, 4, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }

    @Test
    fun `should not place ship for wrong player`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        val result = gameManager.placeShip(999, 0, 0, 4, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
    }

    @Test
    fun `should handle hit and keep turn`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим двухпалубный корабль для p2
        gameManager.placeShip(p2.id, 0, 0, 2, "right")

        // p1 стреляет и попадает (не топит)
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, result)

        // Ход должен остаться у p1
        val game = gameManager.getCurrentGame()
        assertEquals(p1.id, game?.currentPlayer?.id)
    }

    @Test
    fun `should handle kill and keep turn`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим однопалубный корабль для p2
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // p1 стреляет и топит (возвращает KILL)
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.KILL, result)

        // Победитель должен быть определён
        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
    }

    @Test
    fun `should switch turn on miss`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // p1 стреляет в пустую клетку
        val result = gameManager.makeMove(p1.id, 5, 5)
        assertEquals(MoveResult.MISS, result)

        // Ход должен перейти к p2
        val game = gameManager.getCurrentGame()
        assertEquals(p2.id, game?.currentPlayer?.id)
    }

    @Test
    fun `should not allow player to move out of turn`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // p2 пытается сходить (но первый ход у p1)
        val result = gameManager.makeMove(p2.id, 5, 5)
        assertEquals(MoveResult.INVALID, result)
    }

    @Test
    fun `should not allow move after game ended`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим один корабль для p2
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // p1 побеждает
        gameManager.makeMove(p1.id, 0, 0)  // KILL, победитель определён

        // После победы игра должна быть завершена
        assertNotNull(gameManager.getCurrentGame()?.winner)

        // Попытка сходить после победы - INVALID
        val finalResult = gameManager.makeMove(p1.id, 5, 5)
        assertEquals(MoveResult.INVALID, finalResult)
    }

    @Test
    fun `should detect game win`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим один корабль для p2
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // p1 попадает и выигрывает
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.KILL, result)

        // Победитель должен быть определён
        val game = gameManager.getCurrentGame()
        assertEquals(p1.id, game?.winner?.id)
    }

    @Test
    fun `should prevent shooting same cell twice`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Ставим ДВУХпалубный корабль (чтобы игра не закончилась после первого выстрела)
        gameManager.placeShip(p2.id, 0, 0, 2, "right")

        // Первый выстрел - HIT
        val firstResult = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.HIT, firstResult)

        // Пытаемся стрелять в ту же клетку - ALREADY_SHOT
        val secondResult = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.ALREADY_SHOT, secondResult)
    }

    @Test
    fun `should get game stats correctly`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        // Расставляем корабли
        val placeResult1 = gameManager.placeShip(p2.id, 0, 0, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, placeResult1, "Не удалось установить корабль для p2")

        val placeResult2 = gameManager.placeShip(p1.id, 0, 3, 2, "right")
        assertEquals(ShipPlacementResult.SUCCESS, placeResult2, "Не удалось установить корабль для p1")

        // Проверяем статистику ДО выстрела
        val statsBefore = gameManager.getGameStats()
        println("=== СТАТИСТИКА ДО ВЫСТРЕЛА ===")
        println("player1Ships: ${statsBefore.player1Ships}")
        println("player1Hits: ${statsBefore.player1Hits}")
        println("player2Ships: ${statsBefore.player2Ships}")
        println("player2Hits: ${statsBefore.player2Hits}")

        assertEquals(2, statsBefore.player1Ships, "Корабли игрока 1 до выстрела")
        assertEquals(0, statsBefore.player1Hits, "Попадания игрока 1 до выстрела")
        assertEquals(2, statsBefore.player2Ships, "Корабли игрока 2 до выстрела")
        assertEquals(0, statsBefore.player2Hits, "Попадания игрока 2 до выстрела")

        // Получаем доску p2 и проверяем, что корабль установлен
        val game = gameManager.getCurrentGame()
        val p2Board = game?.board2
        println("P2 board: [0][0]=${p2Board?.get(0)?.get(0)} [0][1]=${p2Board?.get(0)?.get(1)}")

        // Делаем выстрел (попадание)
        val moveResult = gameManager.makeMove(p1.id, 0, 0)
        println("Move result: $moveResult")
        assertEquals(MoveResult.HIT, moveResult)

        // Проверяем статистику ПОСЛЕ выстрела
        val statsAfter = gameManager.getGameStats()
        println("=== СТАТИСТИКА ПОСЛЕ ВЫСТРЕЛА ===")
        println("player1Ships: ${statsAfter.player1Ships}")
        println("player1Hits: ${statsAfter.player1Hits}")
        println("player2Ships: ${statsAfter.player2Ships}")
        println("player2Hits: ${statsAfter.player2Hits}")

        assertEquals(2, statsAfter.player1Ships, "Корабли игрока 1 после выстрела")
        assertEquals(1, statsAfter.player1Hits, "Попадания игрока 1 после выстрела")
        assertEquals(1, statsAfter.player2Ships, "Корабли игрока 2 после выстрела (одна клетка осталась)")
        assertEquals(0, statsAfter.player2Hits, "Попадания игрока 2 после выстрела")
    }

    @Test
    fun `should return empty stats when no game`() {
        val stats = gameManager.getGameStats()
        assertEquals(0, stats.player1Id)
        assertEquals("", stats.player1Name)
    }

    @Test
    fun `should check fleet readiness`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        assertFalse(gameManager.isFleetReady(p1.id))

        gameManager.placeShip(p1.id, 0, 0, 4, "right")

        assertFalse(gameManager.isFleetReady(p1.id))

    }
}
