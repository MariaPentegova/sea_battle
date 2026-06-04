package test

import database.DatabaseManager
import models.GameMove
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DatabaseManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var dbManager: DatabaseManager

    @BeforeEach
    fun setUp() {
        val dbFile = tempDir.resolve("test.db").absolutePath
        dbManager = DatabaseManager(dbFile)
        dbManager.initialize()
    }

    // ========== ИНИЦИАЛИЗАЦИЯ БД ==========

    @Test
    fun `should initialize database with all tables`() {
        val conn = dbManager.getConnection()

        val tables = listOf("players", "games", "moves", "player_statistics")
        for (table in tables) {
            val rs = conn.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='$table'"
            ).executeQuery()
            assertTrue(rs.next(), "Table $table should exist")
        }
    }

    // ========== СОХРАНЕНИЕ И ЗАГРУЗКА ИГРОКОВ ==========

    @Test
    fun `should save and load player`() {
        dbManager.savePlayer(1, "Анна")

        val players = dbManager.getAllPlayers()
        assertEquals(1, players.size)
        assertEquals(1, players[0].first)
        assertEquals("Анна", players[0].second)
    }

    @Test
    fun `should save and load multiple players`() {
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")
        dbManager.savePlayer(3, "Светлана")

        val players = dbManager.getAllPlayers()
        assertEquals(3, players.size)
        assertTrue(players.any { it.first == 1 && it.second == "Анна" })
        assertTrue(players.any { it.first == 2 && it.second == "Борис" })
        assertTrue(players.any { it.first == 3 && it.second == "Светлана" })
    }

    @Test
    fun `should update existing player`() {
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(1, "Анна Обновлённая")

        val players = dbManager.getAllPlayers()
        assertEquals(1, players.size)
        assertEquals("Анна Обновлённая", players[0].second)
    }

    // ========== СОХРАНЕНИЕ И ЗАГРУЗКА ИГР С ХОДАМИ ==========

    @Test
    fun `should save and load game with moves`() {
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")

        val moves = listOf(
            GameMove(1, 0, 0, true, false, 1, 1000),
            GameMove(1, 0, 1, true, true, 2, 1010),
            GameMove(2, 5, 5, false, false, 3, 1020)
        )

        dbManager.saveGame(
            gameId = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = moves
        )

        val games = dbManager.getAllGames()
        assertEquals(1, games.size)
        assertEquals("game-001", games[0].id)
        assertEquals(1, games[0].player1Id)
        assertEquals(2, games[0].player2Id)
        assertEquals(1, games[0].winnerId)
        assertEquals(1000, games[0].startTime)
        assertEquals(2000, games[0].endTime)
        assertEquals(3, games[0].moves.size)

        // Проверка первого хода
        assertEquals(1, games[0].moves[0].playerId)
        assertEquals(0, games[0].moves[0].row)
        assertEquals(0, games[0].moves[0].col)
        assertTrue(games[0].moves[0].isHit)
        assertFalse(games[0].moves[0].isKill)

        // Проверка второго хода (потопление)
        assertTrue(games[0].moves[1].isHit)
        assertTrue(games[0].moves[1].isKill)
    }

    @Test
    fun `should get games by player`() {
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")
        dbManager.savePlayer(3, "Светлана")

        dbManager.saveGame("game-001", 1, 2, 1, 1000, 2000, emptyList())
        dbManager.saveGame("game-002", 1, 3, 3, 2000, 3000, emptyList())
        dbManager.saveGame("game-003", 2, 3, 2, 3000, 4000, emptyList())

        val gamesByPlayer1 = dbManager.getGamesByPlayer(1)
        assertEquals(2, gamesByPlayer1.size)

        val gamesByPlayer2 = dbManager.getGamesByPlayer(2)
        assertEquals(2, gamesByPlayer2.size)

        val gamesByPlayer3 = dbManager.getGamesByPlayer(3)
        assertEquals(2, gamesByPlayer3.size)
    }

    // ========== СТАТИСТИКА ==========

    @Test
    fun `should calculate player statistics correctly`() {
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")

        val moves = listOf(
            GameMove(1, 0, 0, true, false, 1, 1000),
            GameMove(1, 0, 1, true, true, 2, 1010),
            GameMove(2, 5, 5, false, false, 3, 1020),
            GameMove(1, 1, 0, true, false, 4, 1030),
            GameMove(1, 1, 1, true, true, 5, 1040)
        )

        dbManager.saveGame("game-001", 1, 2, 1, 1000, 2000, moves)

        val stats = dbManager.getAllPlayerStatistics()
        assertEquals(2, stats.size)

        val annaStats = stats.find { it.playerId == 1 }
        assertNotNull(annaStats)
        assertEquals(1, annaStats?.gamesPlayed)
        assertEquals(5, annaStats?.gamesWon)
        assertEquals(4, annaStats?.totalHits)

        val borisStats = stats.find { it.playerId == 2 }
        assertNotNull(borisStats)
        assertEquals(1, borisStats?.gamesPlayed)
        assertEquals(0, borisStats?.gamesWon)
        assertEquals(0, borisStats?.totalHits)
    }

    @Test
    fun `should get player statistics by id`() {
        dbManager.savePlayer(1, "Анна")

        val moves = listOf(
            GameMove(1, 0, 0, true, false, 1, 1000),
            GameMove(1, 0, 1, true, true, 2, 1010)
        )
        dbManager.saveGame("game-001", 1, 2, 1, 1000, 2000, moves)

        val annaStats = dbManager.getPlayerStatistics(1)
        assertNotNull(annaStats)
        assertEquals(1, annaStats?.gamesPlayed)
        assertEquals(2, annaStats?.gamesWon)
        assertEquals(2, annaStats?.totalHits)
        assertEquals(1, annaStats?.shipsSunk)

        val nonExistentStats = dbManager.getPlayerStatistics(999)
        assertNull(nonExistentStats)
    }

    // ========== ОЧИСТКА ДАННЫХ ==========

    @Test
    fun `should clear all data`() {
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")
        dbManager.saveGame("game-001", 1, 2, 1, 1000, 2000, emptyList())

        dbManager.clearAll()

        assertEquals(0, dbManager.getAllPlayers().size)
        assertEquals(0, dbManager.getAllGames().size)
    }
}
