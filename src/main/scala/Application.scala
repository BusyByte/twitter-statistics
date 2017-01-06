package net.nomadicalien.twitter

import net.nomadicalien.twitter.models.{Delete, DeletedTweet, Status, Tweet}
import net.nomadicalien.twitter.stream.TwitterStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Application extends App {
  import TwitterStream.materializer

  println("starting")
  TwitterStream.twitterStream match {
    case Left(error) => println("Error: " + error.message)
    case Right(tweetSourceF) =>
      tweetSourceF.foreach {
        case Left(error) => println("Error: " + error.message)
        case Right(twitterSource) =>
          val runForEachF = twitterSource.runForeach {
            case Left(error) => println("Error: " + error.message)
            case Right(Tweet(text)) => println(s"Tweet")
            case Right(DeletedTweet(Delete(Status(id)))) => println(s"Deleted tweet")
          }
          Await.result(runForEachF, Duration.Inf)
      }
      Await.result(tweetSourceF, Duration.Inf)
  }

  println("finished")

}
