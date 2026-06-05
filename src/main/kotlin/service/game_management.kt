package service

import models.*
import database.DatabaseManager
import java.util.UUID

class GameManager(
    private val validator: BoardValidator,
    private val battleService: BattleService,
    private val boardFactory: BoardFactory,
    private val dbManager: DatabaseManager
) {
    private var nextPlayerId = 1
    private val players = mutableMapOf<Int, Player>()
    private var currentGame: GameState? = null
    private var currentGameId: String? = null
    private val currentMoves = mutableListOf<GameMove>()
    private var gameStartTime: Long = 0

    init {
        dbManager.initialize()
        loadPlayersFromDb()
    }

    private fun loadPlayersFromDb() {
        val playersFromDb = dbManager.getAllPlayers()
        for ((id, name) in playersFromDb) {
            val player = Player(id, name)
            players[id] = player
            if (id >= nextPlayerId) nextPlayerId = id + 1
        }
    }

    fun addPlayer(name: String): Player {
        val player = Player(nextPlayerId, name.trim())
        players[nextPlayerId] = player
        dbManager.savePlayer(nextPlayerId, player.name)
        nextPlayerId++
        return player
    }

    fun getAllPlayers(): List<Player> = players.values.toList()
    fun getPlayerById(id: Int): Player? = players[id]
    fun getCurrentGame(): GameState? = currentGame

    fun createGame(player1Id: Int, player2Id: Int): GameState? {
        val p1 = players[player1Id]
        val p2 = players[player2Id]
        if (p1 == null || p2 == null || p1.id == p2.id) return null

        currentGameId = UUID.randomUUID().toString()
        currentGame = GameState(p1, p2, boardFactory.createEmptyBoard(), boardFactory.createEmptyBoard(), p1)
        currentMoves.clear()
        gameStartTime = System.currentTimeMillis()

        return currentGame
    }

    fun finishGame() {
        currentGame?.let { game ->
            if (game.winner != null) {
                dbManager.saveGame(
                    gameId = currentGameId!!,
                    player1Id = game.player1.id,
                    player2Id = game.player2.id,
                    winnerId = game.winner?.id,
                    startTime = gameStartTime,
                    endTime = System.currentTimeMillis(),
                    moves = currentMoves.toList()
                )
                println("   Игра сохранена в БД!")
                println("   ID игры: ${currentGameId}")
                println("   Победитель: ${game.winner?.name}")
                println("   Всего ходов: ${currentMoves.size}")
            }
        }
        currentGame = null
        currentGameId = null
        currentMoves.clear()
    }

    fun placeShip(playerId: Int, startRow: Int, startCol: Int, length: Int, direction: String): ShipPlacementResult {
        val game = currentGame ?: return ShipPlacementResult.OUT_OF_BOUNDS
        val player = getPlayerById(playerId) ?: return ShipPlacementResult.OUT_OF_BOUNDS
        val board = game.getBoard(player)

        val result = validator.canPlaceShip(board, startRow, startCol, length, direction)
        if (result == ShipPlacementResult.SUCCESS) {
            validator.placeShip(board, startRow, startCol, length, direction)
        }
        return result
    }

    fun makeMove(playerId: Int, row: Int, col: Int): MoveResult {
        val game = currentGame ?: return MoveResult.INVALID
        if (row !in 0 until BoardValidator.SIZE || col !in 0 until BoardValidator.SIZE) {
            return MoveResult.INVALID
        }
        if (game.winner != null) return MoveResult.INVALID
        if (game.currentPlayer.id != playerId) return MoveResult.INVALID

        val opponent = game.getOpponent(game.currentPlayer)
        val opponentBoard = game.getBoard(opponent)

        val targetCell = opponentBoard[row][col]
        if (targetCell == 'X' || targetCell == '•') return MoveResult.ALREADY_SHOT

        val result = battleService.makeMove(opponentBoard, row, col)

        val isKill = (result == MoveResult.KILL)
        val isHit = (result == MoveResult.HIT || result == MoveResult.KILL)

        currentMoves.add(GameMove(
            playerId = playerId,
            row = row,
            col = col,
            isHit = isHit,
            isKill = isKill,
            moveNumber = currentMoves.size + 1,
            timestamp = System.currentTimeMillis()
        ))

        when (result) {
            MoveResult.HIT, MoveResult.KILL -> {
                if (battleService.isGameOver(opponentBoard)) {
                    game.winner = game.currentPlayer
                    finishGame()
                }
            }
            MoveResult.MISS -> game.switchTurn()
            else -> return result
        }
        return result
    }

    fun getGameStats(): GameStats {
        val game = currentGame ?: return GameStats.empty()
        return GameStats(
            player1Id = game.player1.id,
            player1Name = game.player1.name,
            player1Ships = battleService.getRemainingShipsCount(game.board1),
            player1Hits = battleService.getHitCount(game.board2),
            player2Id = game.player2.id,
            player2Name = game.player2.name,
            player2Ships = battleService.getRemainingShipsCount(game.board2),
            player2Hits = battleService.getHitCount(game.board1),
            currentPlayerId = game.currentPlayer.id,
            currentPlayerName = game.currentPlayer.name
        )
    }

    // ========== МЕТОДЫ ДЛЯ ПРОСМОТРА ИСТОРИИ И СТАТИСТИКИ ==========

    fun getAllGamesFromDb(): List<StoredGame> = dbManager.getAllGames()

    fun getGamesByPlayerFromDb(playerId: Int): List<StoredGame> = dbManager.getGamesByPlayer(playerId)

    fun getAllPlayerStatsFromDb(): List<PlayerStatsRecord> = dbManager.getAllPlayerStatistics()

    fun getPlayerStatsFromDb(playerId: Int): PlayerStatsRecord? = dbManager.getPlayerStatistics(playerId)

    fun isFleetReady(playerId: Int): Boolean {
        val game = currentGame ?: return false
        val player = getPlayerById(playerId) ?: return false
        val board = game.getBoard(player)
        return battleService.getRemainingShipsCount(board) == 20
    }
}
