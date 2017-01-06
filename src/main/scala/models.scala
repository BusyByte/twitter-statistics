package net.nomadicalien.twitter.models


sealed trait TwitterStatusApiModel
final case class Tweet(text:String) extends TwitterStatusApiModel

final case class Status(id: Long)
final case class Delete(status: Status)
final case class DeletedTweet(delete: Delete) extends TwitterStatusApiModel


sealed trait ApplicationError {
  def message: String
}
case class MissingConfigError(message: String) extends ApplicationError
case class HttpError(message: String) extends ApplicationError
case class TweetParseError(message: String) extends ApplicationError