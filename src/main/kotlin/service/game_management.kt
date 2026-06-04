package service

import models.*

class GameManager(
    private val validator: BoardValidator,
    private val battleService: BattleService,
    private val boardFactory: BoardFactory  // Здесь всё правильно
)  {
    private var nextPlayerId = 1
    private val players = mutableMapOf<Int, Player>()
    private var currentGame: GameState? = null

    fun addPlayer(name: String): Player {
        val player = Player(nextPlayerId, name.trim())
        players[nextPlayerId] = player
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

        currentGame = GameState(
            player1 = p1,
            player2 = p2,
            board1 = boardFactory.createEmptyBoard(),
            board2 = boardFactory.createEmptyBoard(),
            currentPlayer = p1
        )
        return currentGame
    }

    fun finishGame() {
        currentGame = null
    }

    fun placeShip(
        playerId: Int,
        startRow: Int,
        startCol: Int,
        length: Int,
        direction: String
    ): ShipPlacementResult {
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

        // НОВАЯ ПРОВЕРКА - не ломает существующие тесты
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

        when (result) {
            MoveResult.HIT, MoveResult.KILL -> {
                if (battleService.isGameOver(opponentBoard)) {
                    game.winner = game.currentPlayer
                }
            }
            MoveResult.MISS -> game.switchTurn()
            else -> return result
        }

        return result
    }

    fun getGameStats(): GameStats {
        val game = currentGame ?: return GameStats.empty()

        // player1Hits - попадания игрока 1 (на доске игрока 2)
        // player2Hits - попадания игрока 2 (на доске игрока 1)
        return GameStats(
            player1Id = game.player1.id,
            player1Name = game.player1.name,
            player1Ships = battleService.getRemainingShipsCount(game.board1),  // корабли игрока 1
            player1Hits = battleService.getHitCount(game.board2),  // ← попадания игрока 1 на доске игрока 2
            player2Id = game.player2.id,
            player2Name = game.player2.name,
            player2Ships = battleService.getRemainingShipsCount(game.board2),  // корабли игрока 2
            player2Hits = battleService.getHitCount(game.board1),  // ← попадания игрока 2 на доске игрока 1
            currentPlayerId = game.currentPlayer.id,
            currentPlayerName = game.currentPlayer.name
        )
    }

    fun isFleetReady(playerId: Int): Boolean {
        val game = currentGame ?: return false
        val player = getPlayerById(playerId) ?: return false
        val board = game.getBoard(player)
        val expectedTotalCells = 20 // 4+3+3+2+2+2+1+1+1+1
        val actualCells = battleService.getRemainingShipsCount(board)
        return actualCells == expectedTotalCells
    }
}
