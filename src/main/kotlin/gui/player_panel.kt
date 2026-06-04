package gui

import models.Player
import service.GameManager
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class PlayersPanel(
    private val gameManager: GameManager,
    private val onPlayersChanged: () -> Unit
) : JPanel() {

    private val playersListModel = DefaultListModel<Player>()
    private val playersList = JList(playersListModel)
    private val addButton = JButton(" Добавить игрока")
    private val deleteButton = JButton(" Удалить")

    init {
        layout = BorderLayout()
        border = EmptyBorder(5, 5, 5, 5)

        setupList()
        setupButtons()
        refresh()
    }

    private fun setupList() {
        playersList.cellRenderer = PlayerListCellRenderer()
        playersList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val scrollPane = JScrollPane(playersList)
        scrollPane.preferredSize = java.awt.Dimension(280, 200)
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun setupButtons() {
        val buttonPanel = JPanel(GridLayout(1, 2, 5, 5))
        buttonPanel.border = EmptyBorder(5, 0, 0, 0)

        addButton.addActionListener { showAddPlayerDialog() }
        deleteButton.addActionListener { deleteSelectedPlayer() }

        buttonPanel.add(addButton)
        buttonPanel.add(deleteButton)

        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun showAddPlayerDialog() {
        val name = JOptionPane.showInputDialog(
            this,
            "Введите имя игрока:",
            "Добавление игрока",
            JOptionPane.QUESTION_MESSAGE
        )

        if (!name.isNullOrBlank()) {
            val trimmedName = name.trim()
            val existing = gameManager.getAllPlayers().find { it.name.equals(trimmedName, ignoreCase = true) }
            if (existing != null) {
                val confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Игрок с именем '${existing.name}' уже существует (ID: ${existing.id}).\nДобавить всё равно?",
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION
                )
                if (confirm != JOptionPane.YES_OPTION) return
            }

            gameManager.addPlayer(trimmedName)
            refresh()
            onPlayersChanged()
        }
    }

    private fun deleteSelectedPlayer() {
        val selected = playersList.selectedValue
        if (selected != null) {
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "Удалить игрока '${selected.name}' (ID: ${selected.id})?\nВсе игры с этим игроком будут потеряны!",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )

            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(
                    this,
                    "Удаление игроков пока не реализовано",
                    "Информация",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    }

    fun refresh() {
        playersListModel.clear()
        gameManager.getAllPlayers().forEach { playersListModel.addElement(it) }
    }

    private class PlayerListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is Player) {
                text = "ID: ${value.id} → ${value.name}"
                font = Font("Monospaced", Font.PLAIN, 12)
            }
            return component
        }
    }
}
