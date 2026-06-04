import gui.MainFrame
import service.*
import ui.ConsoleUI
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main() {
    val choice = JOptionPane.showOptionDialog(
        null,
        "Выберите режим запуска:",
        "Морской Бой",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        arrayOf("GUI", "Консоль"),
        "GUI"
    )

    when (choice) {
        0 -> {
            SwingUtilities.invokeLater {
                val frame = MainFrame()
                frame.isVisible = true
            }
        }
        1 -> {
            val validator = BoardValidator()
            val battleService = BattleService(validator)
            val boardFactory = BoardFactory()
            val gameManager = GameManager(validator, battleService, boardFactory)
            val consoleUI = ConsoleUI(gameManager, boardFactory)
            consoleUI.start()
        }
        else -> {
            println("Выход")
        }
    }
}
