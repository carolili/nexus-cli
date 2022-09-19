package ch.epfl.bluebrain.nexus.cli

import ch.epfl.bluebrain.nexus.cli.sdk.{BearerToken, Label}
import fs2.io.file.Path
import io.circe.Json
import org.http4s.Uri

sealed trait Intent extends Product with Serializable

object Intent {
  case object ShowLogin   extends Intent
  case object RemoveLogin extends Intent
  case class Login(
      endpoint: Option[Uri],
      realm: Option[Label],
      token: Option[BearerToken],
      clientId: String
  ) extends Intent
}
