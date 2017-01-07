package net.nomadicalien.twitter

import net.nomadicalien.twitter.models.{Delete, DeletedTweet, Status, Tweet}
import net.nomadicalien.twitter.stream.TwitterStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Application extends App {
  import TwitterStream.materializer

  println("starting")
  val streamFinishedF = TwitterStream.twitterStream.runForeach {
    case Left(error) => System.err.println("Error: " + error.message)
    case Right(Tweet(text)) => println(s"Tweet $text")
    case Right(DeletedTweet(Delete(Status(id)))) => println(s"Deleted tweet $id")
  }
  println("awaiting stream termination")
  Await.result(streamFinishedF, Duration.Inf)
  System.err.println("stream terminated")
}
