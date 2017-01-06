package net.nomadicalien.twitter.models


final case class Tweet(text:String)

sealed trait ApplicationError {
  def message: String
}
case class MissingConfigError(message: String) extends ApplicationError
case class OAuthError(message: String) extends ApplicationError