package test

import models.MoveResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import service.*
import database.DatabaseManager
import java.io.File

class SystemTest {

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

    // ========== СИСТЕМНЫЙ ТЕСТ 1: ПОЛНАЯ ИГРА ОТ НАЧАЛА ДО ПОБЕДЫ ==========

    @Test
    fun `complete game from start to win`() {
        // 1. Добавляем игроков
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        // 2. Создаём партию
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)

        // 3. Расставляем корабли (минимальные для быстрой победы)
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // 4. Игрок Анна стреляет и побеждает
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.KILL || result == MoveResult.GAME_WON)

        // 5. Проверяем, что победитель сохранился в БД
        val games = gameManager.getAllGamesFromDb()
        assertTrue(games.isNotEmpty())

        val savedGame = games.first()
        assertEquals(p1.id, savedGame.winnerId)
        assertNotNull(savedGame.endTime)
    }

    // ========== СИСТЕМНЫЙ ТЕСТ 2: ИГРОКИ С ОДИНАКОВЫМИ ИМЕНАМИ ==========

    @Test
    fun `players with same names can play against each other`() {
        // 1. Добавляем двух игроков с одинаковыми именами
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Анна")

        // 2. Создаём партию между ними
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)

        // 3. Расставляем корабли
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // 4. Играем (Анна1 стреляет в Анну2)
        val result = gameManager.makeMove(p1.id, 0, 0)
        assertTrue(result == MoveResult.KILL || result == MoveResult.GAME_WON)

        // 5. Проверяем, что победитель — Анна1 (ID=1)
        val games = gameManager.getAllGamesFromDb()
        val savedGame = games.first()
        assertEquals(p1.id, savedGame.winnerId)
    }

    // ========== СИСТЕМНЫЙ ТЕСТ 3: СОХРАНЕНИЕ ИСТОРИИ ПОСЛЕ ИГРЫ ==========

    @Test
    fun `game history is saved after completion`() {
        // 1. Добавляем игроков
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")

        // 2. Создаём партию
        gameManager.createGame(p1.id, p2.id)

        // 3. Расставляем корабли
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // 4. Играем
        gameManager.makeMove(p1.id, 0, 0)

        // 5. Проверяем, что игра сохранилась в БД с правильными данными
        val games = gameManager.getAllGamesFromDb()
        assertEquals(1, games.size)

        val savedGame = games.first()
        assertEquals(p1.id, savedGame.player1Id)
        assertEquals(p2.id, savedGame.player2Id)
        assertEquals(p1.id, savedGame.winnerId)
        assertTrue(savedGame.moves.isNotEmpty())
        assertNotNull(savedGame.endTime)
    }
}
