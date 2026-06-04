package test

import database.*
import models.GameMove
import models.StoredGame
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PlayerStatsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var dbManager: DatabaseManager
    private lateinit var statsRepo: PlayerStatsRepository
    private lateinit var gameHistoryRepo: GameHistoryRepository

    @BeforeEach
    fun setUp() {
        val dbFile = tempDir.resolve("test.db").absolutePath
        dbManager = DatabaseManager(dbFile)
        dbManager.initialize()
        statsRepo = PlayerStatsRepository(dbManager)
        gameHistoryRepo = GameHistoryRepository(dbManager)

        // Добавляем тестовых игроков
        dbManager.savePlayer(1, "Анна")
        dbManager.savePlayer(2, "Борис")
        dbManager.savePlayer(3, "Светлана")
        dbManager.savePlayer(4, "Дмитрий")
    }

    // ========== ПЕРЕСЧЁТ СТАТИСТИКИ ==========

    @Test
    fun `should refresh statistics after games`() {
        // Игра 1: Анна побеждает Бориса
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = listOf(
                GameMove(1, 0, 0, true, true, 1, 1500),
                GameMove(2, 5, 5, false, false, 2, 1600)
            )
        )
        gameHistoryRepo.saveGame(game1)
        statsRepo.refreshStatistics()

        val annaStats = statsRepo.getPlayerStats(1)
        assertNotNull(annaStats)
        assertEquals(1, annaStats?.gamesPlayed)
        assertEquals(1, annaStats?.gamesWon)
        assertEquals(1, annaStats?.totalHits)
        assertEquals(1, annaStats?.shipsSunk)

        val borisStats = statsRepo.getPlayerStats(2)
        assertNotNull(borisStats)
        assertEquals(1, borisStats?.gamesPlayed)
        assertEquals(0, borisStats?.gamesWon)
        assertEquals(0, borisStats?.totalHits)
    }

    @Test
    fun `should accumulate statistics over multiple games`() {
        // Игра 1: Анна побеждает Бориса
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = listOf(
                GameMove(1, 0, 0, true, false, 1, 1500),
                GameMove(1, 0, 1, true, true, 2, 1510),
                GameMove(2, 5, 5, false, false, 3, 1520)
            )
        )
        gameHistoryRepo.saveGame(game1)

        // Игра 2: Анна побеждает Светлану
        val game2 = StoredGame(
            id = "game-002",
            player1Id = 1,
            player2Id = 3,
            winnerId = 1,
            startTime = 3000,
            endTime = 4000,
            moves = listOf(
                GameMove(1, 2, 2, true, true, 1, 3500),
                GameMove(3, 7, 7, false, false, 2, 3510)
            )
        )
        gameHistoryRepo.saveGame(game2)

        statsRepo.refreshStatistics()

        val allStats = statsRepo.getAllPlayerStats()

        // Выводим для отладки
        allStats.forEach { stat ->
            println("Player ${stat.playerId} (${stat.playerName}): games=${stat.gamesPlayed}, wins=${stat.gamesWon}, hits=${stat.totalHits}")
        }

        val annaStats = allStats.find { it.playerId == 1 }
        assertNotNull(annaStats)

        // Анна сыграла 2 игры, выиграла 2
        assertEquals(2, annaStats?.gamesPlayed)
        assertEquals(2, annaStats?.gamesWon)

        // Попадания Анны: 2 (первая игра) + 1 (вторая игра) = 3
        // Если у тебя получается 6, значит дублируются ходы
        assertEquals(3, annaStats?.totalHits)

        // Потоплено кораблей: 1 (первая игра) + 1 (вторая игра) = 2
        assertEquals(2, annaStats?.shipsSunk)
    }

    @Test
    fun `should handle player with no games`() {
        // Дмитрий (id=4) не играл ни одной игры
        statsRepo.refreshStatistics()

        val dmitryStats = statsRepo.getPlayerStats(4)
        assertNotNull(dmitryStats)
        assertEquals(0, dmitryStats?.gamesPlayed)
        assertEquals(0, dmitryStats?.gamesWon)
        assertEquals(0, dmitryStats?.totalHits)
        assertEquals(0, dmitryStats?.shipsSunk)
    }

    // ========== ПОЛУЧЕНИЕ ВСЕХ СТАТИСТИК ==========

    @Test
    fun `should get all player stats ordered by wins`() {
        // Создаём игры с разными победителями
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = listOf(GameMove(1, 0, 0, true, true, 1, 1500))
        )
        gameHistoryRepo.saveGame(game1)

        val game2 = StoredGame(
            id = "game-002",
            player1Id = 1,
            player2Id = 3,
            winnerId = 1,
            startTime = 2000,
            endTime = 3000,
            moves = listOf(GameMove(1, 1, 1, true, true, 1, 2500))
        )
        gameHistoryRepo.saveGame(game2)

        val game3 = StoredGame(
            id = "game-003",
            player1Id = 2,
            player2Id = 3,
            winnerId = 2,
            startTime = 3000,
            endTime = 4000,
            moves = listOf(GameMove(2, 2, 2, true, true, 1, 3500))
        )
        gameHistoryRepo.saveGame(game3)

        statsRepo.refreshStatistics()

        val allStats = statsRepo.getAllPlayerStats()
        assertEquals(4, allStats.size)  // 4 игрока (включая Дмитрия с 0 игр)

        // Первый должен быть Анна (2 победы)
        assertEquals(1, allStats[0].playerId)
        assertEquals(2, allStats[0].gamesWon)

        // Второй должен быть Борис (1 победа)
        assertEquals(2, allStats[1].playerId)
        assertEquals(1, allStats[1].gamesWon)

        // Третий — Светлана (0 побед)
        assertEquals(3, allStats[2].playerId)
        assertEquals(0, allStats[2].gamesWon)
    }

    // ========== ПРОВЕРКА WINRATE ==========

    @Test
    fun `should calculate win rate correctly`() {
        val game1 = StoredGame(
            id = "game-001",
            player1Id = 1,
            player2Id = 2,
            winnerId = 1,
            startTime = 1000,
            endTime = 2000,
            moves = emptyList()
        )
        gameHistoryRepo.saveGame(game1)

        val game2 = StoredGame(
            id = "game-002",
            player1Id = 1,
            player2Id = 2,
            winnerId = 2,
            startTime = 2000,
            endTime = 3000,
            moves = emptyList()
        )
        gameHistoryRepo.saveGame(game2)

        statsRepo.refreshStatistics()

        val annaStats = statsRepo.getPlayerStats(1)
        assertNotNull(annaStats)
        assertEquals(2, annaStats?.gamesPlayed)
        assertEquals(1, annaStats?.gamesWon)
        assertEquals(0.5f, annaStats?.winRate)

        val borisStats = statsRepo.getPlayerStats(2)
        assertNotNull(borisStats)
        assertEquals(2, borisStats?.gamesPlayed)
        assertEquals(1, borisStats?.gamesWon)
        assertEquals(0.5f, borisStats?.winRate)
    }
}
