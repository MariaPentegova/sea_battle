package gui

import models.Player
import service.*
import database.DatabaseManager
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder

class MainFrame(private val dbManager: DatabaseManager) : JFrame("Морской Бой") {

    companion object {
        private const val PANEL_MAIN = "main"
        private const val PANEL_GAME = "game"
    }

    private val validator = BoardValidator()
    private val battleService = BattleService(validator)
    private val boardFactory = BoardFactory()
    private val gameManager = GameManager(validator, battleService, boardFactory, dbManager)

    private val cardLayout = CardLayout()
    private val mainPanel = JPanel(cardLayout)

    private val playersPanel = PlayersPanel(gameManager) { refreshPlayersList() }
    private val statsPanel = StatsPanel(gameManager)
    private var gameBoardPanel: GameBoardPanel? = null

    private var gameTimer: Timer? = null
    private var startTime: Long = 0
    private val timerLabel = JLabel("Время: 0 сек")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent) {
                val confirm = JOptionPane.showConfirmDialog(
                    this@MainFrame,
                    "Вы уверены, что хотите выйти?",
                    "Выход",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0)
                }
            }
        })
        preferredSize = Dimension(1200, 800)
        layout = BorderLayout()

        createMenuBar()
        setupMainPanel()

        pack()
        setLocationRelativeTo(null)
    }

    private fun createMenuBar() {
        val menuBar = JMenuBar()

        val gameMenu = JMenu("Игра")
        val newGameItem = JMenuItem("Новая игра")
        newGameItem.addActionListener { startNewGame() }
        val exitItem = JMenuItem("Выход")
        exitItem.addActionListener {
            dispose()
            System.exit(0)
        }

        gameMenu.add(newGameItem)
        gameMenu.addSeparator()
        gameMenu.add(exitItem)

        val helpMenu = JMenu("Помощь")
        val hintItem = JMenuItem("Как играть")
        hintItem.addActionListener {
            JOptionPane.showMessageDialog(
                this,
                "Правила игры:\n" +
                        "1. Каждый игрок расставляет свои корабли\n" +
                        "2. Игроки ходят по очереди, стреляя по клеткам противника\n" +
                        "3. При попадании игрок ходит ещё раз\n" +
                        "4. Побеждает тот, кто первым потопит все корабли противника\n\n" +
                        "Цвета на доске:\n" +
                        " Синий - ваш корабль\n" +
                        " Красный - попадание\n" +
                        " Серый - промах",
                "Помощь",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        val aboutItem = JMenuItem("О программе")
        aboutItem.addActionListener { showAboutDialog() }

        helpMenu.add(hintItem)
        helpMenu.addSeparator()
        helpMenu.add(aboutItem)

        menuBar.add(gameMenu)
        menuBar.add(helpMenu)
        jMenuBar = menuBar
    }

    private fun setupMainPanel() {
        val leftPanel = JPanel(BorderLayout())
        leftPanel.preferredSize = Dimension(300, 0)
        leftPanel.border = EmptyBorder(10, 10, 10, 10)

        val playersScrollPane = JScrollPane(playersPanel)
        playersScrollPane.border = BorderFactory.createTitledBorder("Игроки")

        val statsScrollPane = JScrollPane(statsPanel)
        statsScrollPane.border = BorderFactory.createTitledBorder("База данных (SQLite)")
        statsScrollPane.preferredSize = Dimension(280, 400)

        leftPanel.add(playersScrollPane, BorderLayout.CENTER)
        leftPanel.add(statsScrollPane, BorderLayout.SOUTH)

        val centerPanel = JPanel(BorderLayout())
        centerPanel.border = EmptyBorder(10, 10, 10, 10)

        val topBar = JPanel(BorderLayout())
        topBar.add(timerLabel, BorderLayout.WEST)

        val statusLabel = JLabel("Выберите игроков и нажмите 'Новая игра'")
        statusLabel.font = Font("Arial", Font.BOLD, 14)
        topBar.add(statusLabel, BorderLayout.CENTER)

        centerPanel.add(topBar, BorderLayout.NORTH)

        val tempPanel = JPanel()
        tempPanel.add(JLabel("Нажмите 'Новая игра' для начала"))
        centerPanel.add(tempPanel, BorderLayout.CENTER)

        mainPanel.add(leftPanel, PANEL_MAIN)
        mainPanel.add(centerPanel, PANEL_MAIN)

        add(mainPanel, BorderLayout.CENTER)
    }

    private fun startNewGame() {
        val players = gameManager.getAllPlayers()
        if (players.size < 2) {
            JOptionPane.showMessageDialog(
                this,
                "Для игры нужно минимум 2 игрока!\nСейчас: ${players.size}",
                "Ошибка",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val p1 = selectPlayer(players, "первого") ?: return
        val p2 = selectPlayer(players.filter { it.id != p1.id }, "второго") ?: return

        val game = gameManager.createGame(p1.id, p2.id)
        if (game == null) {
            JOptionPane.showMessageDialog(this, "Не удалось создать игру", "Ошибка", JOptionPane.ERROR_MESSAGE)
            return
        }

        val p1Success = setupShipsForPlayer(p1)
        if (!p1Success) {
            gameManager.finishGame()
            showMainView()
            return
        }

        val p2Success = setupShipsForPlayer(p2)
        if (!p2Success) {
            gameManager.finishGame()
            showMainView()
            return
        }

        val newGamePanel = GameBoardPanel(gameManager, game, this::onGameEnd, this::updateStats)
        gameBoardPanel = newGamePanel

        mainPanel.add(newGamePanel, PANEL_GAME)
        cardLayout.show(mainPanel, PANEL_GAME)

        startTimer()
        updateStats()
    }

    private fun selectPlayer(players: List<Player>, order: String): Player? {
        val names = players.map { "${it.id}: ${it.name}" }.toTypedArray()
        val choice = JOptionPane.showOptionDialog(
            this,
            "Выберите $order игрока:",
            "Выбор игрока",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            names,
            names[0]
        )
        return if (choice >= 0) players[choice] else null
    }

    private fun setupShipsForPlayer(player: Player): Boolean {
        val fleet = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
        var isCancelled = false

        val dialog = ShipPlacementDialog(
            parent = this,
            gameManager = gameManager,
            player = player,
            fleet = fleet,
            onCancel = {
                isCancelled = true
            }
        )
        dialog.isVisible = true

        if (isCancelled || dialog.isCancelled()) {
            JOptionPane.showMessageDialog(
                this,
                "Игрок ${player.name} отменил расстановку.\nИгра будет завершена!",
                "Игра отменена",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }

        if (!gameManager.isFleetReady(player.id)) {
            JOptionPane.showMessageDialog(
                this,
                "Игрок ${player.name} не расставил корабли!",
                "Ошибка",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }

        return true
    }

    private fun onGameEnd(winner: Player) {
        stopTimer()
        JOptionPane.showMessageDialog(
            this,
            "ПОБЕДА! \nИгрок ${winner.name} (ID: ${winner.id}) выиграл!",
            "Игра завершена",
            JOptionPane.INFORMATION_MESSAGE
        )
        refreshPlayersList()
        statsPanel.refresh()
        showMainView()
    }

    private fun updateStats() {
        val stats = gameManager.getGameStats()
        timerLabel.text = "Время: ${getElapsedSeconds()} сек | " +
                "${stats.player1Name}: ${stats.player1Ships} клеток | " +
                "${stats.player2Name}: ${stats.player2Ships} клеток"
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        gameTimer = Timer(1000) {
            timerLabel.text = "Время: ${getElapsedSeconds()} сек"
            updateStats()
        }
        gameTimer?.start()
    }

    private fun stopTimer() {
        gameTimer?.stop()
        gameTimer = null
    }

    private fun getElapsedSeconds(): Long {
        return (System.currentTimeMillis() - startTime) / 1000
    }

    private fun refreshPlayersList() {
        playersPanel.refresh()
        statsPanel.refresh()
    }

    private fun showMainView() {
        cardLayout.show(mainPanel, PANEL_MAIN)
    }

    private fun showAboutDialog() {
        JOptionPane.showMessageDialog(
            this,
            "Морской Бой\nВерсия 2.0\n\nРазработано с использованием Kotlin",
            "О программе",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}
