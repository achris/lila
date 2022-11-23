package lila.round

import lila.game.Game
import lila.hub.actorApi.timeline.{ GameEnd as TLGameEnd, Propagate }
import lila.notify.{ GameEnd, NotifyApi }
import lila.user.User

final private class RoundNotifier(
    timeline: lila.hub.actors.Timeline,
    isUserPresent: (Game, User.ID) => Fu[Boolean],
    notifyApi: NotifyApi
)(using ec: scala.concurrent.ExecutionContext):

  def gameEnd(game: Game)(color: chess.Color) =
    if (!game.aborted) game.player(color).userId foreach { userId =>
      game.perfType foreach { perfType =>
        timeline ! (Propagate(
          TLGameEnd(
            playerId = game fullIdOf color,
            opponent = game.player(!color).userId,
            win = game.winnerColor map (color ==),
            perf = perfType.key
          )
        ) toUser userId)
      }
      isUserPresent(game, userId) foreach {
        case false =>
          notifyApi.notifyOne(
            userId,
            GameEnd(
              GameId(game fullIdOf color),
              game.opponent(color).userId,
              game.wonBy(color)
            )
          )
        case _ =>
      }
    }
