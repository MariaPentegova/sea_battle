package gui

import models.Player
import models.ShipPlacementResult
import service.GameManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.TitledBorder

class ShipPlacementDialog(
    parent: JFrame,
    private val gameManager: GameManager,
    private val player: Player,
    private val fleet: List<Int>,
    private val onCancel: () -> Unit  // ← колбэк при отмене/закрытии
) : JDialog(parent, "Расстановка кораблей - ${player.name}", true) {

    private val boardButtons = Array(10) { arrayOfNulls<JButton>(10) }
    private var currentShipIndex = 0
    private var shipNumber = 1
    private var isCancelled = false

    private val statusLabel = JLabel()
    private val shipInfoLabel = JLabel()

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        setSize(600, 650)
        setLocationRelativeTo(parent)
        layout = BorderLayout()

        // Обработчик закрытия окна (крестик)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                val confirm = JOptionPane.showConfirmDialog(
                    this@ShipPlacementDialog,
                    "Вы уверены, что хотите закрыть окно расстановки?\n" +
                            "Игрок ${player.name} не сможет участвовать в игре.\n" +
                            "Игра будет завершена!",
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    isCancelled = true
                    dispose()
                    onCancel()
                }
            }
        })

        setupTopPanel()
        setupBoard()
        startPlacement()
    }

    private fun setupTopPanel() {
        val topPanel = JPanel()
        topPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val titleLabel = JLabel("Игрок: ${player.name} (ID: ${player.id})", SwingConstants.CENTER)
        titleLabel.font = Font("Arial", Font.BOLD, 16)

        shipInfoLabel.font = Font("Arial", Font.BOLD, 14)
        shipInfoLabel.foreground = Color.BLUE

        statusLabel.font = Font("Arial", Font.PLAIN, 12)
        statusLabel.foreground = Color.RED

        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.add(titleLabel)
        topPanel.add(Box.createVerticalStrut(10))
        topPanel.add(shipInfoLabel)
        topPanel.add(Box.createVerticalStrut(5))
        topPanel.add(statusLabel)

        add(topPanel, BorderLayout.NORTH)
    }

    private fun setupBoard() {
        val boardPanel = JPanel(GridBagLayout())
        boardPanel.border = TitledBorder("Ваше поле")

        val gbc = GridBagConstraints()

        gbc.gridx = 1
        gbc.gridy = 0
        for (col in 0..9) {
            val label = JLabel((col + 1).toString(), SwingConstants.CENTER)
            label.preferredSize = Dimension(45, 25)
            boardPanel.add(label, gbc)
            gbc.gridx++
        }

        for (row in 0..9) {
            gbc.gridx = 0
            gbc.gridy = row + 1
            val rowLabel = JLabel(('A' + row).toString(), SwingConstants.CENTER)
            rowLabel.preferredSize = Dimension(30, 45)
            boardPanel.add(rowLabel, gbc)

            gbc.gridx = 1
            for (col in 0..9) {
                val button = createBoardCell(row, col)
                boardButtons[row][col] = button
                boardPanel.add(button, gbc)
                gbc.gridx++
            }
        }

        add(boardPanel, BorderLayout.CENTER)
    }

    private fun createBoardCell(row: Int, col: Int): JButton {
        val button = JButton()
        button.preferredSize = Dimension(45, 45)
        button.isFocusPainted = false

        button.addActionListener {
            if (isCancelled) return@addActionListener
            if (currentShipIndex < fleet.size) {
                val length = fleet[currentShipIndex]
                val direction = getDirection(length)
                if (direction != null) {
                    placeShip(row, col, length, direction)
                }
            }
        }

        return button
    }

    private fun getDirection(shipLength: Int): String? {
        if (shipLength == 1) return "right"

        val options = arrayOf("➡ Вправо", "⬇ Вниз", "⬅ Влево", "⬆ Вверх")
        val choice = JOptionPane.showOptionDialog(
            this,
            "Выберите направление для корабля длиной $shipLength",
            "Направление",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )
        return when (choice) {
            0 -> "right"
            1 -> "down"
            2 -> "left"
            3 -> "up"
            else -> null
        }
    }

    private fun placeShip(row: Int, col: Int, length: Int, direction: String) {
        val result = gameManager.placeShip(player.id, row, col, length, direction)

        when (result) {
            ShipPlacementResult.SUCCESS -> {
                updateBoardDisplay()
                currentShipIndex++
                shipNumber++

                if (currentShipIndex < fleet.size) {
                    statusLabel.text = "✓ Корабль установлен!"
                    shipInfoLabel.text = "Следующий корабль: ${fleet[currentShipIndex]} палубы (${shipNumber}/${fleet.size})"
                    Timer(1500) { statusLabel.text = "" }.start()
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "✓ Все корабли расставлены!\nИгрок ${player.name} готов к бою!",
                        "Готово",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    isVisible = false
                    dispose()
                }
            }
            ShipPlacementResult.OUT_OF_BOUNDS -> statusLabel.text = "Выход за границы поля!"
            ShipPlacementResult.OVERLAP -> statusLabel.text = "Пересечение с другим кораблём!"
            ShipPlacementResult.TOO_CLOSE -> statusLabel.text = "Слишком близко к другому кораблю (нужен зазор 1 клетку)!"
            ShipPlacementResult.INVALID_DIRECTION -> statusLabel.text = "Неверное направление!"
        }

        revalidate()
        repaint()
    }

    private fun updateBoardDisplay() {
        val board = gameManager.getCurrentGame()?.getBoard(player) ?: return

        for (i in 0..9) {
            for (j in 0..9) {
                val button = boardButtons[i][j] ?: continue
                val cell = board[i][j]

                button.text = if (cell == '■') "■" else ""
                button.foreground = if (cell == '■') Color.BLUE else Color.BLACK
                button.background = if (cell == '■') Color.CYAN else null
            }
        }
    }

    private fun startPlacement() {
        shipInfoLabel.text = "Корабль ${shipNumber}/${fleet.size}: ${fleet[currentShipIndex]} палубы"
        statusLabel.text = "Нажмите на клетку для установки корабля"
        updateBoardDisplay()
    }

    fun isCancelled(): Boolean = isCancelled
}
