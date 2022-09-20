package ch.epfl.bluebrain.nexus.cli

import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import ch.epfl.bluebrain.nexus.cli.impl.Config
import ch.epfl.bluebrain.nexus.cli.impl.LoginConfig.{AnonymousLoginConfig, UserLoginConfig}
import ch.epfl.bluebrain.nexus.cli.sdk.api.Api
import ch.epfl.bluebrain.nexus.cli.sdk.{Err, Terminal}
import com.monovore.decline.Help
import org.http4s.blaze.client.BlazeClientBuilder

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    Terminal()
      .use { terminal =>
        val io: IO[ExitCode] = CliOpts.command.parse(args) match {
          case Left(help)    =>
            pluginInvocation(args, help) match {
              case None                         => CliOpts.printHelp(terminal, help).as(ExitCode.Success)
              case Some(_)                      => CliOpts.printHelp(terminal, help).as(ExitCode.Success)
            }
          case Right(intent) => evaluate(intent, terminal)
        }

        io.handleErrorWith {
          case e: Err => e.println(terminal).as(ExitCode.Error)
          case th     => Err.UnknownErr(th).println(terminal).as(ExitCode.Error)
        }
      }

  private def evaluate(intent: Intent, term: Terminal): IO[ExitCode] =
    intent match {
      case Intent.Login(endpoint, realm, token, clientId) => impl.Login(term, endpoint, realm, token, clientId)
      case Intent.ShowLogin                               => impl.ShowLogin(term)
      case Intent.RemoveLogin                             => impl.RemoveLogin()
    }

  private def apiResource: Resource[IO, Api] =
    BlazeClientBuilder[IO].resource.flatMap { client =>
      val apiIO = Config.load.flatMap {
        case Some(UserLoginConfig(endpoint, _, token, _)) => IO.pure(Api(client, endpoint, token))
        case Some(AnonymousLoginConfig(endpoint))         => IO.pure(Api(client, endpoint))
        case None                                         => IO.raiseError(Err.UnconfiguredErr)
      }
      Resource.eval(apiIO)
    }

  private def pluginInvocation(args: List[String], help: Help): Option[(String, List[String])] =
    (args, help.errors.headOption) match {
      case (head :: rest, Some(err)) if err == s"Unexpected argument: $head" => Some((head, rest))
      case _                                                                 => None
    }
}
