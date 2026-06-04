package test

import models.MoveResult
import models.ShipPlacementResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.*

class SystemTest {
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
    fun `system - complete game with two players from creation to victory`() {
        // Полный цикл: добавление игроков → создание игры → расстановка → битва → победа
        val p1 = gameManager.addPlayer("Капитан")
        val p2 = gameManager.addPlayer("Пират")
        
        val game = gameManager.createGame(p1.id, p2.id)
        assertNotNull(game)

        // Расстановка флотов (по 2 корабля для скорости)
        gameManager.placeShip(p1.id, 0, 0, 2, "right")
        gameManager.placeShip(p1.id, 3, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 5, 2, "right")
        gameManager.placeShip(p2.id, 3, 5, 1, "right")

        // Битва до победы
        gameManager.makeMove(p1.id, 0, 5)  // попадание
        gameManager.makeMove(p1.id, 0, 6)  // потопил (KILL)
        gameManager.makeMove(p1.id, 3, 5)  // потопил (KILL) - победа

        val finalGame = gameManager.getCurrentGame()
        assertNotNull(finalGame?.winner)
        assertEquals(p1.id, finalGame?.winner?.id)
    }

    @Test
    fun `system - invalid actions are rejected throughout game lifecycle`() {
        // Проверка, что система корректно обрабатывает ошибки пользователя
        val p1 = gameManager.addPlayer("Игрок1")
        val p2 = gameManager.addPlayer("Игрок2")
        gameManager.createGame(p1.id, p2.id)

        // Неверная расстановка кораблей
        var result = gameManager.placeShip(p1.id, -1, 0, 3, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)
        
        result = gameManager.placeShip(p1.id, 9, 9, 2, "right")
        assertEquals(ShipPlacementResult.OUT_OF_BOUNDS, result)

        // Правильная расстановка
        gameManager.placeShip(p2.id, 0, 0, 1, "right")

        // Неверный ход (не тот игрок)
        var moveResult = gameManager.makeMove(p2.id, 0, 0)
        assertEquals(MoveResult.INVALID, moveResult)  // ходит p1, а пытается p2

        // Повторный выстрел в ту же клетку
        gameManager.makeMove(p1.id, 0, 0)  // HIT или KILL
        moveResult = gameManager.makeMove(p1.id, 0, 0)
        assertEquals(MoveResult.ALREADY_SHOT, moveResult)

        // Выстрел после победы невозможен
        val game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
        moveResult = gameManager.makeMove(p1.id, 5, 5)
        assertEquals(MoveResult.INVALID, moveResult)
    }

    @Test
    fun `system - multiple consecutive games without restart`() {
        // Проверка, что система позволяет играть несколько игр подряд
        val p1 = gameManager.addPlayer("Анна")
        val p2 = gameManager.addPlayer("Борис")
        val p3 = gameManager.addPlayer("Светлана")

        // Игра 1: Анна vs Борис
        gameManager.createGame(p1.id, p2.id)
        gameManager.placeShip(p1.id, 0, 0, 1, "right")
        gameManager.placeShip(p2.id, 0, 0, 1, "right")
        gameManager.makeMove(p1.id, 0, 0)
        
        var game = gameManager.getCurrentGame()
        assertNotNull(game?.winner)
        gameManager.finishGame()

        // Игра 2: Борис vs Светлана (с новыми игроками)
        val newGame = gameManager.createGame(p2.id, p3.id)
        assertNotNull(newGame)
        assertEquals(p2.id, newGame?.player1?.id)
        assertEquals(p3.id, newGame?.player2?.id)
        
        // Старые игроки (Анна) всё ещё в списке
        assertEquals(3, gameManager.getAllPlayers().size)
    }
}
