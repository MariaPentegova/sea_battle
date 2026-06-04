package gui

import models.GameState
import models.MoveResult
import models.Player
import service.GameManager
import service.BoardFactory
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.TitledBorder

class GameBoardPanel(
    private val gameManager: GameManager,
    private val gameState: GameState,
    private val onGameEnd: (Player) -> Unit,
    private val onStatsUpdate: () -> Unit
) : JPanel() {

    private val boardFactory = BoardFactory()

    private val player1MyBoard = Array(10) { arrayOfNulls<JButton>(10) }
    private val player1EnemyBoard = Array(10) { arrayOfNulls<JButton>(10) }

    private val player2MyBoard = Array(10) { arrayOfNulls<JButton>(10) }
    private val player2EnemyBoard = Array(10) { arrayOfNulls<JButton>(10) }

    private val currentPlayerLabel = JLabel()
    private val statusLabel = JLabel()

    init {
        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        val infoPanel = createInfoPanel()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        add(infoPanel, gbc)

        val player1Panel = createPlayerPanel(gameState.player1, gameState.player2, player1MyBoard, player1EnemyBoard)
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        add(player1Panel, gbc)

        val player2Panel = createPlayerPanel(gameState.player2, gameState.player1, player2MyBoard, player2EnemyBoard)
        gbc.gridx = 1
        gbc.gridy = 1
        add(player2Panel, gbc)

        updateAllBoards()
        updateTurnInfo()
    }

    private fun createPlayerPanel(
        currentPlayer: Player,
        opponent: Player,
        myBoardButtons: Array<Array<JButton?>>,
        enemyBoardButtons: Array<Array<JButton?>>
    ): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("${currentPlayer.name} (ID: ${currentPlayer.id})")

        val gbc = GridBagConstraints()

        val enemyBoardPanel = createBoardPanel("Доска противника: ${opponent.name}", enemyBoardButtons, isMyBoard = false)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.BOTH
        panel.add(enemyBoardPanel, gbc)

        val myBoardPanel = createBoardPanel("Мои корабли", myBoardButtons, isMyBoard = true)
        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(myBoardPanel, gbc)

        return panel
    }

    private fun createBoardPanel(
        title: String,
        buttons: Array<Array<JButton?>>,
        isMyBoard: Boolean
    ): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder(title)

        val gbc = GridBagConstraints()

        gbc.gridx = 1
        gbc.gridy = 0
        for (col in 0..9) {
            val label = JLabel((col + 1).toString(), SwingConstants.CENTER)
            label.preferredSize = Dimension(40, 25)
            panel.add(label, gbc)
            gbc.gridx++
        }

        for (row in 0..9) {
            gbc.gridx = 0
            gbc.gridy = row + 1
            val rowLabel = JLabel(('A' + row).toString(), SwingConstants.CENTER)
            rowLabel.preferredSize = Dimension(25, 40)
            panel.add(rowLabel, gbc)

            gbc.gridx = 1
            for (col in 0..9) {
                val button = JButton()
                button.preferredSize = Dimension(40, 40)
                button.isFocusPainted = false

                if (!isMyBoard) {
                    button.addActionListener {
                        makeMove(row, col)
                    }
                }

                buttons[row][col] = button
                panel.add(button, gbc)
                gbc.gridx++
            }
        }

        return panel
    }

    private fun makeMove(row: Int, col: Int) {
        val currentPlayer = gameState.currentPlayer

        if (gameState.winner != null) {
            statusLabel.text = "Игра уже закончена!"
            return
        }

        val result = gameManager.makeMove(currentPlayer.id, row, col)

        when (result) {
            MoveResult.HIT -> {
                statusLabel.text = " ПОПАДАНИЕ! ${currentPlayer.name} стреляет ещё раз!"
                updateAllBoards()
                onStatsUpdate()
            }
            MoveResult.KILL -> {
                statusLabel.text = " ПОПАДАНИЕ!  КОРАБЛЬ ПОТОПЛЕН! ${currentPlayer.name} стреляет ещё раз!"
                updateAllBoards()
                onStatsUpdate()
                if (gameState.winner != null) {
                    onGameEnd(gameState.winner!!)
                }
            }
            MoveResult.MISS -> {
                statusLabel.text = " ПРОМАХ! Ход переходит к ${gameState.getOpponent(currentPlayer).name}"
                updateAllBoards()
                updateTurnInfo()
                onStatsUpdate()
            }
            MoveResult.ALREADY_SHOT -> {
                statusLabel.text = " Сюда уже стреляли!"
            }
            MoveResult.INVALID -> {
                statusLabel.text = " Неверный ход!"
            }
            MoveResult.GAME_WON -> {
                statusLabel.text = " ПОБЕДА! ${currentPlayer.name} выиграл! "
                updateAllBoards()
                onStatsUpdate()
                if (gameState.winner != null) {
                    onGameEnd(gameState.winner!!)
                }
            }
        }

        updateTurnInfo()

        if (gameState.winner != null) {
            onGameEnd(gameState.winner!!)
        }
    }

    private fun updateAllBoards() {
        updateBoardDisplay(gameState.board1, player1MyBoard, isMyBoard = true, forPlayer = gameState.player1)
        updateBoardDisplay(gameState.board2, player1EnemyBoard, isMyBoard = false, forPlayer = gameState.player1)

        updateBoardDisplay(gameState.board2, player2MyBoard, isMyBoard = true, forPlayer = gameState.player2)
        updateBoardDisplay(gameState.board1, player2EnemyBoard, isMyBoard = false, forPlayer = gameState.player2)
    }

    private fun updateBoardDisplay(
        sourceBoard: Array<Array<Char>>,
        targetButtons: Array<Array<JButton?>>,
        isMyBoard: Boolean,
        forPlayer: Player
    ) {
        for (i in 0..9) {
            for (j in 0..9) {
                val button = targetButtons[i][j] ?: continue
                val cell = sourceBoard[i][j]

                when (cell) {
                    '■' -> {
                        if (isMyBoard) {
                            button.text = "■"
                            button.foreground = Color.BLUE
                            button.background = Color.CYAN
                            button.isEnabled = false
                        } else {
                            button.text = ""
                            button.background = null
                            button.isEnabled = (gameState.currentPlayer.id == forPlayer.id) && gameState.winner == null
                        }
                    }
                    'X' -> {
                        button.text = "!"
                        button.foreground = Color.WHITE
                        button.background = Color.RED
                        button.isEnabled = false
                    }
                    '•' -> {
                        button.text = "·"
                        button.foreground = Color.GRAY
                        button.background = Color.DARK_GRAY
                        button.isEnabled = false
                    }
                    else -> {
                        button.text = ""
                        button.background = null
                        button.isEnabled = !isMyBoard && (gameState.currentPlayer.id == forPlayer.id) && gameState.winner == null
                    }
                }
            }
        }
    }

    private fun createInfoPanel(): JPanel {
        val panel = JPanel()
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        currentPlayerLabel.font = Font("Arial", Font.BOLD, 16)
        currentPlayerLabel.foreground = Color.BLUE

        statusLabel.font = Font("Arial", Font.PLAIN, 12)
        statusLabel.foreground = Color.RED

        panel.add(currentPlayerLabel)
        panel.add(Box.createHorizontalStrut(20))
        panel.add(statusLabel)

        return panel
    }

    private fun updateTurnInfo() {
        if (gameState.winner != null) {
            currentPlayerLabel.text = " ИГРА ОКОНЧЕНА! Победитель: ${gameState.winner!!.name} (ID: ${gameState.winner!!.id}) 🏆"
            currentPlayerLabel.foreground = Color.GREEN
        } else {
            currentPlayerLabel.text = " ХОДИТ: ${gameState.currentPlayer.name} (ID: ${gameState.currentPlayer.id})"
            currentPlayerLabel.foreground = Color.BLUE
        }
    }
}
