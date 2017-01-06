package net.nomadicalien.twitter

import net.nomadicalien.twitter.stream.TwitterStream

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends App {
  import TwitterStream.materializer

  println("starting")
  TwitterStream.twitterStream match {
    case Left(error) => println("Error: " + error.message)
    case Right(tweetSourceF) =>
      tweetSourceF.foreach { tweetSource =>
        tweetSource.async.runForeach {tweet =>
          println("Tweet: " + tweet.text)
        }

      }
  }

  println("finished")

}
