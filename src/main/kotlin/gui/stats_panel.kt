package gui

import service.GameManager
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel

class StatsPanel(private val gameManager: GameManager) : JPanel() {

    private val statsTableModel = DefaultTableModel(
        arrayOf("ID", "Игрок", "Игр", "Побед", "% побед", "Попаданий", "Кораблей"), 0
    )
    private val statsTable = JTable(statsTableModel)
    private val refreshButton = JButton(" Обновить статистику")

    private val historyTableModel = DefaultTableModel(
        arrayOf("ID игры", "Игрок 1", "Игрок 2", "Победитель", "Дата", "Ходов"), 0
    )
    private val historyTable = JTable(historyTableModel)
    private val refreshHistoryButton = JButton(" Обновить историю")

    private val tabbedPane = JTabbedPane()

    private val collapseButton = JButton("▼ Свернуть")
    private val contentPanel = JPanel(BorderLayout())
    private var isCollapsed = false

    init {
        layout = BorderLayout()
        border = EmptyBorder(5, 5, 5, 5)

        setupCollapsiblePanel()
        setupTables()
        setupButtons()

        refreshStats()
        refreshHistory()
    }

    private fun setupCollapsiblePanel() {
        val titlePanel = JPanel(BorderLayout())
        val titleLabel = JLabel(" СТАТИСТИКА И ИСТОРИЯ", SwingConstants.CENTER)
        titleLabel.font = Font("Arial", Font.BOLD, 14)

        titlePanel.add(collapseButton, BorderLayout.WEST)
        titlePanel.add(titleLabel, BorderLayout.CENTER)

        collapseButton.addActionListener {
            isCollapsed = !isCollapsed
            contentPanel.isVisible = !isCollapsed
            collapseButton.text = if (isCollapsed) " Развернуть" else " Свернуть"
        }

        add(titlePanel, BorderLayout.NORTH)
        contentPanel.border = EmptyBorder(5, 0, 0, 0)
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun setupTables() {
        statsTable.font = Font("Monospaced", Font.PLAIN, 11)
        statsTable.rowHeight = 20
        statsTable.getTableHeader().reorderingAllowed = false

        val statsScrollPane = JScrollPane(statsTable)
        statsScrollPane.border = BorderFactory.createTitledBorder(" Статистика игроков")
        statsScrollPane.preferredSize = java.awt.Dimension(280, 250)

        historyTable.font = Font("Monospaced", Font.PLAIN, 11)
        historyTable.rowHeight = 20
        historyTable.getTableHeader().reorderingAllowed = false

        val historyScrollPane = JScrollPane(historyTable)
        historyScrollPane.border = BorderFactory.createTitledBorder(" История партий")
        historyScrollPane.preferredSize = java.awt.Dimension(280, 250)

        tabbedPane.addTab("Статистика", statsScrollPane)
        tabbedPane.addTab("История игр", historyScrollPane)

        contentPanel.add(tabbedPane, BorderLayout.CENTER)
    }

    private fun setupButtons() {
        val buttonPanel = JPanel()
        buttonPanel.add(refreshButton)
        buttonPanel.add(refreshHistoryButton)

        refreshButton.addActionListener { refreshStats() }
        refreshHistoryButton.addActionListener { refreshHistory() }

        contentPanel.add(buttonPanel, BorderLayout.SOUTH)
    }

    fun refreshStats() {
        statsTableModel.setRowCount(0)
        val stats = gameManager.getAllPlayerStatsFromDb()

        stats.forEach { stat ->
            statsTableModel.addRow(arrayOf(
                stat.playerId,
                stat.playerName,
                stat.gamesPlayed,
                stat.gamesWon,
                "${(stat.winRate * 100).toInt()}%",
                stat.totalHits,
                stat.shipsSunk
            ))
        }
    }

    fun refreshHistory() {
        historyTableModel.setRowCount(0)
        val games = gameManager.getAllGamesFromDb()

        games.forEach { game ->
            val player1 = gameManager.getPlayerById(game.player1Id)?.name ?: "?"
            val player2 = gameManager.getPlayerById(game.player2Id)?.name ?: "?"
            val winner = game.winnerId?.let { gameManager.getPlayerById(it)?.name } ?: "?"
            val date = java.util.Date(game.startTime).toString().take(20)

            historyTableModel.addRow(arrayOf(
                game.id.take(8) + "...",
                player1,
                player2,
                winner,
                date,
                game.moves.size
            ))
        }
    }

    fun refresh() {
        refreshStats()
        refreshHistory()
    }
}
