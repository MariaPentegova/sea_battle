package test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import service.*
import ui.ConsoleUI
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SystemTest {

    @Test
    fun `system - complete game from creation to victory via console`() {
        val input = """
            1
            Капитан
            1
            Пират
            3
            1
            2
            A1
            right
            A3
            right
            A5
            right
            B1
            right
            B3
            right
            B5
            right
            C1
            right
            C3
            right
            C5
            right
            D1
            right
            2
            1
            A1
            right
            A3
            right
            A5
            right
            B1
            right
            B3
            right
            B5
            right
            C1
            right
            C3
            right
            C5
            right
            D1
            right
            A1
            stats
            A1
            5
        """.trimIndent()

        val validator = BoardValidator()
        val battleService = BattleService(validator)
        val boardFactory = BoardFactory()
        val gameManager = GameManager(validator, battleService, boardFactory)

        System.setIn(ByteArrayInputStream(input.toByteArray()))
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val consoleUI = ConsoleUI(gameManager, boardFactory)
        val thread = Thread { consoleUI.start() }
        thread.start()
        thread.join(8000)

        val output = outputStream.toString()
        assertTrue(output.contains("ПОБЕДА") || output.contains("победил") || output.contains("ИГРА ЗАВЕРШЕНА"))

        System.setIn(System.`in`)
        System.setOut(System.out)
    }

    @Test
    fun `system - console handles invalid input gracefully`() {
        val input = """
            1
            Тест
            3
            1
            2
            Z99
            A1
            right
            X99
            A1
            right
            5
        """.trimIndent()

        val validator = BoardValidator()
        val battleService = BattleService(validator)
        val boardFactory = BoardFactory()
        val gameManager = GameManager(validator, battleService, boardFactory)

        System.setIn(ByteArrayInputStream(input.toByteArray()))
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val consoleUI = ConsoleUI(gameManager, boardFactory)
        val thread = Thread { consoleUI.start() }
        thread.start()
        thread.join(5000)

        val output = outputStream.toString()
        assertTrue(output.contains("Неверный формат") || output.contains("Ошибка") || output.contains("Неверный"))

        System.setIn(System.`in`)
        System.setOut(System.out)
    }

    @Test
    fun `system - player can add and view players before game`() {
        val input = """
            1
            Анна
            1
            Борис
            2
            5
        """.trimIndent()

        val validator = BoardValidator()
        val battleService = BattleService(validator)
        val boardFactory = BoardFactory()
        val gameManager = GameManager(validator, battleService, boardFactory)

        System.setIn(ByteArrayInputStream(input.toByteArray()))
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val consoleUI = ConsoleUI(gameManager, boardFactory)
        val thread = Thread { consoleUI.start() }
        thread.start()
        thread.join(4000)

        val output = outputStream.toString()
        assertTrue(output.contains("Анна") && output.contains("Борис"))
        assertTrue(output.contains("СПИСОК ИГРОКОВ") || output.contains("ID:"))

        System.setIn(System.`in`)
        System.setOut(System.out)
    }
}
