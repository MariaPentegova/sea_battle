package gui

import service.GameManager
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel

class StatsPanel(private val gameManager: GameManager) : JPanel() {

    private val tableModel = DefaultTableModel(
        arrayOf("ID", "Игрок", "Корабли (тек. игра)", "Попадания"), 0
    )
    private val statsTable = JTable(tableModel)
    private val refreshButton = JButton("🔄 Обновить")

    init {
        layout = BorderLayout()
        border = EmptyBorder(5, 5, 5, 5)

        statsTable.font = Font("Monospaced", Font.PLAIN, 11)
        statsTable.rowHeight = 20
        statsTable.tableHeader.reorderingAllowed = false

        val scrollPane = JScrollPane(statsTable)
        scrollPane.preferredSize = java.awt.Dimension(280, 300)

        refreshButton.addActionListener { refresh() }

        add(scrollPane, BorderLayout.CENTER)
        add(refreshButton, BorderLayout.SOUTH)

        refresh()
    }

    fun refresh() {
        tableModel.setRowCount(0)

        val players = gameManager.getAllPlayers()
        val game = gameManager.getCurrentGame()
        val stats = if (game != null) gameManager.getGameStats() else null

        players.forEach { player ->
            val shipsLeft = when {
                stats == null -> 0
                player.id == stats.player1Id -> stats.player1Ships
                player.id == stats.player2Id -> stats.player2Ships
                else -> 0
            }
            val hits = when {
                stats == null -> 0
                player.id == stats.player1Id -> stats.player1Hits
                player.id == stats.player2Id -> stats.player2Hits
                else -> 0
            }
            
            tableModel.addRow(arrayOf(
                player.id,
                player.name,
                shipsLeft,
                hits
            ))
        }
    }
}
