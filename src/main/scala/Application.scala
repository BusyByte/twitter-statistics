package net.nomadicalien.twitter

import net.nomadicalien.twitter.models.{Delete, DeletedTweet, Status, Tweet}
import net.nomadicalien.twitter.stream.TwitterStream
import org.apache.logging.log4j.LogManager

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Application extends App {
  val logger = LogManager.getLogger(this.getClass)

  import TwitterStream.materializer

  logger.info("starting")
  val streamFinishedF = TwitterStream.twitterStream.runForeach {
    case Left(error) => logger.error("Error: " + error.message)
    case Right(Tweet(text)) => logger.info(s"Tweet $text")
    case Right(DeletedTweet(Delete(Status(id)))) => logger.info(s"Deleted tweet $id")
  }
  logger.info("awaiting stream termination")
  Await.result(streamFinishedF, Duration.Inf)
  logger.error("stream terminated")
  TwitterStream.shutdown()
}
