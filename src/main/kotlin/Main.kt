import service.*
import ui.ConsoleUI

fun main() {
    val validator = BoardValidator()
    val battleService = BattleService(validator)
    val boardFactory = BoardFactory()
    val gameManager = GameManager(validator, battleService, boardFactory)
    val consoleUI = ConsoleUI(gameManager, boardFactory)

    consoleUI.start()
}
