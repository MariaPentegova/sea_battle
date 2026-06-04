package test

import models.MoveResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import service.*
import database.DatabaseManager
import java.io.File

class IntegrationTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var validator: BoardValidator
    private lateinit var battleService: BattleService
    private lateinit var factory: BoardFactory
    private lateinit var dbManager: DatabaseManager
    private lateinit var gameManager: GameManager

    @BeforeEach
    fun setUp() {
        val dbFile = tempDir.resolve("test.db").absolutePath
        dbManager = DatabaseManager(dbFile)
        dbManager.initialize()

        validator = BoardValidator()
        battleService = BattleService(validator)
        factory = BoardFactory()
        gameManager = GameManager(validator, battleService, factory, dbManager)
    }

    // ========== ПРОВЕРКА ПЕРЕЗАПУСКА ==========

    @Test
    fun `game persistence after restart - players loaded from database`() {
        // Первая сессия
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        assertEquals(2, gameManager.getAllPlayers().size)

        // "Перезапуск" — создаём новый GameManager с той же БД
        val newGameManager = GameManager(validator, battleService, factory, dbManager)
        val loadedPlayers = newGameManager.getAllPlayers()

        assertEquals(2, loadedPlayers.size)
        assertTrue(loadedPlayers.any { it.name == "Анна" })
        assertTrue(loadedPlayers.any { it.name == "Борис" })
    }

    // ========== ПРОВЕРКА ПОБЕДЫ ==========

    @Test
    fun `complete battle with win detection and database save`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        gameManager.createGame(p1.id, p2.id)

        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.KILL || result == MoveResult.GAME_WON)

        val games = gameManager.getAllGamesFromDb()
        assertTrue(games.isNotEmpty())

        val savedGame = games.first()
        assertEquals(p1.id, savedGame.winnerId)
        assertNotNull(savedGame.endTime)
    }

    // ========== ПРОВЕРКА СТАТИСТИКИ ==========

    @Test
    fun `statistics survive application restart`() {
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        gameManager.createGame(p1.id, p2.id)
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.makeMove(p1.id, 0, 0)

        // Перезапуск
        val newGameManager = GameManager(validator, battleService, factory, dbManager)
        val stats = newGameManager.getAllPlayerStatsFromDb()

        val annaStats = stats.find { it.playerId == p1.id }
        assertEquals(1, annaStats?.gamesPlayed)
        assertEquals(1, annaStats?.gamesWon)
    }
}
