package test

import models.GameMove
import models.StoredGame
import database.GameHistoryRepository
import database.DatabaseManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GameHistoryRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var dbManager: DatabaseManager
    private lateinit var repository: GameHistoryRepository

    @BeforeEach
    fun setUp() {
        val dbFile = tempDir.resolve("test.db").absolutePath
        dbManager = DatabaseManager(dbFile)
        dbManager.initialize()
        repository = GameHistoryRepository(dbManager)

        // Добавляем тестовых игроков
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")
        dbManager.savePlayer(3, "Светлана")
    }

    // ========== СОХРАНЕНИЕ И ЗАГРУЗКА ИГР ==========

    @Test
    fun `should save and load game`() {
        val moves = listOf(
            GameMove(1, 0, 0, true, false, 1, 1000),
            GameMove(1, 0, 1, true, true, 2, 1010),
            GameMove(2, 5, 5, false, false, 3, 1020)
        )

        val game = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = moves
        )

        repository.saveGame(game)
        val games = repository.getAllGames()

        assertEquals(1, games.size)
        assertEquals("game-001", games[0].id)
        assertEquals(1, games[0].player1Id)
        assertEquals(2, games[0].player2Id)
        assertEquals(1, games[0].winnerId)
        assertEquals(1000, games[0].startTime)
        assertEquals(2000, games[0].endTime)
        assertEquals(3, games[0].moves.size)
    }

    @Test
    fun `should save and load game without winner`() {
        val game = StoredGame(
            id = "game-002",
            player1Id = 1,
            player2Id = 2,
            winnerId = null,
            startTime = 1000,
            endTime = null,
            moves = emptyList()
        )

        repository.saveGame(game)
        val games = repository.getAllGames()

        assertEquals(1, games.size)
        assertNull(games[0].winnerId)
        assertNull(games[0].endTime)
    }

    @Test
    fun `should update existing game`() {
        // Сохраняем игру
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = null,
            startTime = 1000,
            endTime = null,
            moves = emptyList()
        )
        repository.saveGame(game1)

        // Обновляем игру (добавляем победителя и ходы)
        val moves = listOf(GameMove(1, 0, 0, true, true, 1, 1500))
        val game2 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 1500,
            moves = moves
        )
        repository.saveGame(game2)

        val games = repository.getAllGames()
        assertEquals(1, games.size)
        assertEquals(1, games[0].winnerId)
        assertEquals(1500, games[0].endTime)
        assertEquals(1, games[0].moves.size)
    }

    // ========== ПОИСК ИГР ПО ИГРОКУ ==========

    @Test
    fun `should get games by player`() {
        // Игра 1: Анна (1) vs Борис (2)
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = emptyList()
        )
        repository.saveGame(game1)

        // Игра 2: Анна (1) vs Светлана (3)
        val game2 = StoredGame(
            id = "game-002",
            player1Id = 1,
            player2Id = 3,
            winnerId = 3,
            startTime = 2000,
            endTime = 3000,
            moves = emptyList()
        )
        repository.saveGame(game2)

        // Игра 3: Борис (2) vs Светлана (3)
        val game3 = StoredGame(
            id = "game-003",
            player1Id = 2,
            player2Id = 3,
            winnerId = 2,
            startTime = 3000,
            endTime = 4000,
            moves = emptyList()
        )
        repository.saveGame(game3)

        val annaGames = repository.getGamesByPlayer(1)
        assertEquals(2, annaGames.size)
        assertTrue(annaGames.any { it.id == "game-001" })
        assertTrue(annaGames.any { it.id == "game-002" })

        val borisGames = repository.getGamesByPlayer(2)
        assertEquals(2, borisGames.size)
        assertTrue(borisGames.any { it.id == "game-001" })
        assertTrue(borisGames.any { it.id == "game-003" })

        val svetlanaGames = repository.getGamesByPlayer(3)
        assertEquals(2, svetlanaGames.size)
        assertTrue(svetlanaGames.any { it.id == "game-002" })
        assertTrue(svetlanaGames.any { it.id == "game-003" })

        val nonExistentGames = repository.getGamesByPlayer(999)
        assertEquals(0, nonExistentGames.size)
    }

    // ========== ПРОВЕРКА ПОРЯДКА ==========

    @Test
    fun `should return games sorted by start time descending`() {
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 3000,
            endTime = 4000,
            moves = emptyList()
        )
        repository.saveGame(game1)

        val game2 = StoredGame(
            id = "game-002",
            player1Id = 1,
            player2Id = 2,
            winnerId = 2,
            startTime = 1000,
            endTime = 2000,
            moves = emptyList()
        )
        repository.saveGame(game2)

        val game3 = StoredGame(
            id = "game-003",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 2000,
            endTime = 3000,
            moves = emptyList()
        )
        repository.saveGame(game3)

        val games = repository.getAllGames()
        assertEquals(3, games.size)

        // Проверяем порядок: от новых к старым
        assertTrue(games[0].startTime > games[1].startTime)
        assertTrue(games[1].startTime > games[2].startTime)
    }
}
