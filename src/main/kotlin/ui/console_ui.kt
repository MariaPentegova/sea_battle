package ui

import models.*
import service.*

class ConsoleUI(
    private val gameManager: GameManager,
    private val boardFactory: BoardFactory
) {

    fun start() {
        println("         МОРСКОЙ БОЙ")

        while (true) {
            showMainMenu()
            when (readLine()?.lowercase()) {
                "1" -> addPlayer()
                "2" -> showPlayers()
                "3" -> startNewGame()
                "4" -> showCurrentGameStats()
                "5" -> {
                    println("До свидания!")
                    return
                }
                else -> println("Неверный выбор")
            }
        }
    }

    private fun showMainMenu() {
        println(" ГЛАВНОЕ МЕНЮ")
        println(" 1. Добавить игрока                 ")
        println(" 2. Показать всех игроков           ")
        println(" 3. Начать новую игру               ")
        println(" 4. Показать статистику текущей игры")
        println(" 5. Выход                           ")
        print("Выберите действие: ")
    }

    private fun addPlayer() {
        println("\nВведите имя нового игрока:")
        print("→ ")
        val name = readLine()?.trim()
        if (name.isNullOrEmpty()) {
            println("Имя не может быть пустым")
            return
        }

        val existing = gameManager.getAllPlayers().find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            println("Игрок с именем '${existing.name}' уже существует")
            print("Добавить всё равно? (y/n): ")
            val answer = readLine()?.lowercase()
            when (answer) {
                "y", "yes", "да" -> {
                    println("Добавление игрока с повторяющимся именем")
                }
                "n", "no", "нет" -> {
                    println("Добавление отменено")
                    return
                }
                else -> {
                    println("Неверный ввод. Добавление отменено.")
                    return
                }
            }
        }

        val player = gameManager.addPlayer(name)
        println("Игрок добавлен: (ID: ${player.id}, ${player.name})")
    }

    private fun showPlayers() {
        val players = gameManager.getAllPlayers()
        if (players.isEmpty()) {
            println("\n Список игроков пуст")
            return
        }

        println("         СПИСОК ИГРОКОВ            ")
        players.forEach { player ->
            println(" ID: ${player.id} → ${player.name}")
        }
        println("Всего игроков: ${players.size}")
    }

    private fun startNewGame() {
        val players = gameManager.getAllPlayers()
        if (players.size < 2) {
            println("\nНужно минимум 2 игрока. Сейчас: ${players.size}")
            return
        }

        if (gameManager.getCurrentGame() != null) {
            println("\nУже есть активная игра")
            print("Завершить её? (y/n): ")
            if (readLine()?.lowercase() == "y") {
                gameManager.finishGame()
            } else {
                return
            }
        }

        println("         ВЫБОР ИГРОКОВ")

        val p1 = selectPlayer(players, "первого") ?: return
        val p2 = selectPlayer(players.filter { it.id != p1.id }, "второго") ?: return

        val game = gameManager.createGame(p1.id, p2.id)
        if (game == null) {
            println("Не удалось создать игру")
            return
        }

        println("\nИгра создана!")

        setupFleetWithPlayerSwitch(game.player1, game.player2)

        playBattle(game)
        gameManager.finishGame()
    }

    private fun setupFleetWithPlayerSwitch(player1: Player, player2: Player) {
        val fleet = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

        // Расстановка для первого игрока
        println("\n Первый игрок. Расстановка кораблей для: (ID: ${player1.id}, ${player1.name})")
        println("(Пожалуйста, передайте устройство этому игроку)")
        waitForEnter("Нажмите Enter, когда игрок готов...")

        setupFleetForPlayer(player1, gameManager.getCurrentGame()!!.board1, fleet)

        // Очистка экрана (имитация)
        repeat(50) { println() }

        // Расстановка для второго игрока
        println("\nВторой игрок. Расстановка кораблей для: (ID: ${player2.id}, ${player2.name})")
        println("(Передача устройства этому игроку)")
        waitForEnter("Нажмите Enter, когда игрок готов...")

        setupFleetForPlayer(player2, gameManager.getCurrentGame()!!.board2, fleet)

        repeat(50) { println() }
        println("\n Оба игрока расставили корабли! ")
    }

    private fun setupFleetForPlayer(player: Player, board: Array<Array<Char>>, fleet: List<Int>) {
        var shipNum = 1
        for (length in fleet) {
            println("\nКорабль $shipNum/${fleet.size} ($length палуб)")

            var placed = false
            while (!placed) {
                printBoard(board, showShips = true)

                val (row, col) = getValidCoordinates() ?: continue

                val direction = if (length == 1) "right" else getValidDirection()

                when (gameManager.placeShip(player.id, row, col, length, direction)) {
                    ShipPlacementResult.SUCCESS -> {
                        println("Установлен!")
                        placed = true
                        shipNum++
                    }
                    ShipPlacementResult.OUT_OF_BOUNDS -> println("Выход за границы поля (0-9)")
                    ShipPlacementResult.OVERLAP -> println("Пересечение с другим кораблём")
                    ShipPlacementResult.TOO_CLOSE -> println("Слишком близко (нужен зазор 1 клетку)")
                    ShipPlacementResult.INVALID_DIRECTION -> println("Неверное направление")
                }
            }
        }

        println("\nКорабли игрока (ID: ${player.id}, ${player.name}) расставлены!")
        printBoard(board, showShips = true)
        waitForEnter()
    }

    private fun playBattle(game: GameState) {
        println("          НАЧАЛО БИТВЫ!")
        println("(ID: ${game.player1.id}, ${game.player1.name}) vs (ID: ${game.player2.id}, ${game.player2.name})")
        println("\n Команды во время игры:")
        println(" 'stats' - показать статистику")
        println(" 'exit'  - выйти из игры (без сохранения)")
        println(" 'help'  - показать это сообщение")

        while (game.winner == null) {
            println(" ХОД ИГРОКА: (ID: ${game.currentPlayer.id}, ${game.currentPlayer.name})")

            val opponent = game.getOpponent(game.currentPlayer)
            val hiddenBoard = boardFactory.getHiddenBoard(game.getBoard(opponent))
            printBoard(hiddenBoard, showShips = false)

            var validMove = false
            while (!validMove) {
                print("Выстрел (A1-J10), 'stats', 'exit' или 'help': ")
                val input = readLine()?.lowercase() ?: continue

                when (input) {
                    "stats" -> {
                        showCurrentGameStats()
                        continue
                    }
                    "exit" -> {
                        println("\nИгра прервана. Прогресс не сохранён.")
                        println("Нажмите Enter для возврата в главное меню...")
                        readLine()
                        return
                    }
                    "help" -> {
                        println("\n Команды:")
                        println("   A1-J10 - выстрел по координатам")
                        println("   stats  - показать статистику")
                        println("   exit   - выйти из игры")
                        println("   help   - показать это сообщение")
                        continue
                    }
                }

                val parsed = parseCoord(input)
                if (parsed == null) {
                    println("Неверный формат. Пример: A5")
                    continue
                }

                val (row, col) = parsed

                val result = gameManager.makeMove(game.currentPlayer.id, row, col)

                when (result) {
                    MoveResult.HIT -> {
                        println("\nПОПАДАНИЕ! Ещё выстрел!")
                        validMove = true
                    }
                    MoveResult.KILL -> {
                        println("\nПОПАДАНИЕ! КОРАБЛЬ ПОТОПЛЕН! Ещё выстрел!")
                        validMove = true
                    }
                    MoveResult.MISS -> {
                        println("\nПРОМАХ! Ход переходит к другому игроку!")
                        validMove = true
                    }
                    MoveResult.GAME_WON -> {
                        println("\nПОБЕДА!")
                        println("Игрок (ID: ${game.currentPlayer.id}, ${game.currentPlayer.name}) выиграл!")
                        game.winner = game.currentPlayer
                        validMove = true
                    }
                    MoveResult.ALREADY_SHOT -> {
                        println("Сюда уже стреляли!")
                    }
                    MoveResult.INVALID -> {
                        println("Неверный ход!")
                    }
                }
            }

            if (game.winner == null) {
                val stats = gameManager.getGameStats()
                println("СТАТИСТИКА: ${stats.player1Name}: ${stats.player1Ships} кораблей vs ${stats.player2Name}: ${stats.player2Ships} кораблей")
            }
        }

        println("\nИГРА ЗАВЕРШЕНА!")
        waitForEnter("Нажмите Enter для возврата в главное меню...")
    }

    private fun showCurrentGameStats() {
        val game = gameManager.getCurrentGame()
        if (game == null) {
            println("\n Нет активной игры")
            return
        }

        val stats = gameManager.getGameStats()
        println("         СТАТИСТИКА ИГРЫ")
        println(" ${stats.player1Name} (ID: ${stats.player1Id})")
        println("Кораблей: ${stats.player1Ships} | Попаданий: ${stats.player1Hits}")
        println(" ${stats.player2Name} (ID: ${stats.player2Id})")
        println("Кораблей: ${stats.player2Ships} | Попаданий: ${stats.player2Hits}")
        println("Ходит: ${stats.currentPlayerName}")
    }

    private fun selectPlayer(players: List<Player>, order: String): Player? {
        println("\nВыберите $order игрока:")
        players.forEachIndexed { idx, p -> println("${idx + 1}. (ID: ${p.id}, ${p.name})") }
        println("0. Отмена")
        print("→ ")

        val choice = readLine()?.toIntOrNull()
        if (choice == 0) return null
        if (choice != null && choice in 1..players.size) return players[choice - 1]

        println("Неверный выбор")
        return selectPlayer(players, order)
    }

    private fun getValidCoordinates(): Pair<Int, Int>? {
        while (true) {
            print("Координаты (A1-J10): ")
            val input = readLine()
            if (input == null) {
                println("Ошибка ввода")
                continue
            }
            val parsed = parseCoord(input)
            if (parsed == null) {
                println("Неверный формат. Пример: A5")
                continue
            }
            return parsed
        }
    }

    private fun getValidDirection(): String {
        while (true) {
            print("Направление (up/down/left/right): ")
            val input = readLine()
            if (input == null) {
                println("Ошибка ввода")
                continue
            }
            val dir = input.lowercase()
            if (dir in listOf("up", "down", "left", "right")) {
                return dir
            }
            println("Неверное направление")
        }
    }

    private fun printBoard(board: Array<Array<Char>>, showShips: Boolean) {
        println("\n   1  2  3  4  5  6  7  8  9  10")
        for (i in 0 until 10) {
            print('A' + i)
            print(" ")
            for (j in 0 until 10) {
                val cell = board[i][j]
                val symbol = when {
                    !showShips && cell == '■' -> '~'
                    cell == '~' -> '·'
                    else -> cell
                }
                print(" $symbol ")
            }
            println()
        }
        println(if (showShips) "  ■ - корабль | X - подбит | · - пусто" else "  X - попадание | · - пусто/мимо")
    }

    private fun parseCoord(input: String): Pair<Int, Int>? {
        val trimmed = input.trim().uppercase()
        val regex = Regex("^([A-J])([1-9]|10)$")
        val match = regex.matchEntire(trimmed)

        return if (match != null) {
            val row = match.groupValues[1][0] - 'A'
            val col = match.groupValues[2].toInt() - 1
            if (row in 0..9 && col in 0..9) row to col else null
        } else null
    }

    private fun waitForEnter(message: String = "Нажмите Enter для продолжения...") {
        print(message)
        readLine()
    }
}
