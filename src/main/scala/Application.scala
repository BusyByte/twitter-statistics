package net.nomadicalien.twitter

import akka.stream.scaladsl.{Flow, Sink}
import net.nomadicalien.twitter.models._
import net.nomadicalien.twitter.stream.TwitterStream
import org.apache.logging.log4j.LogManager

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}

object Application extends App {
  val logger = LogManager.getLogger(this.getClass)

  import TwitterStream.materializer
  import net.nomadicalien.twitter.models.Statistics.Implicits.{tweetToStats, statisticsMonoid, showStatistic}

  logger.info("starting")
  val apiToTweetStream = Flow[Either[ApplicationError, TwitterStatusApiModel]]
    .filter {
      case Right(_: Tweet) => true
      case Right(StreamWarning(warning)) =>
        logger.warn("Warning: " + warning)
        false
      case Left(error) =>
        logger.error("Error: " + error.message)
        false
      case _ => false
    }
    .collect {
      case Right(t: Tweet) => tweetToStats.convert(t)
    }
    .filter {
      case Right(t: Statistics) => true
      case Left(error) =>
        logger.error("Error: " + error.message)
        false
    }
    .collect {
      case Right(s : Statistics) => s
    }

  val fiveSecondTreatStream = Flow[Statistics].groupedWithin(Int.MaxValue, 5.seconds)
    .map{ groupedStats: Seq[Statistics] =>
      val results = groupedStats.foldLeft(statisticsMonoid.empty)(statisticsMonoid.combine)
      //logger.info(s"Current 5 second tweet count ${showStatistic.show(results)}")
      results
    }

  def groupAndFold(groupSize: Int, context: String, logStats: Boolean) =
    Flow[Statistics]
      .grouped(groupSize)
      .map { groupedStats =>
        val results = groupedStats.foldLeft(statisticsMonoid.empty)(statisticsMonoid.combine)
        if(logStats) {
          logger.info(s"Current $context tweet count ${showStatistic.show(results)}")
        }
        results
      }

  val fifteenSecondTweetStream = groupAndFold(3, "15 second", false)
  val oneMinuteTweetStream = groupAndFold(4, "1 minute", false)
  val fiveMinuteTweetStream = groupAndFold(5, "5 minute", true)
  val tenMinuteTweetStream = groupAndFold(2, "10 minute", true)
  val twentyMinuteTweetStream = groupAndFold(2, "20 minute", true)
  val oneHourTweetStream = groupAndFold(3, "1 hour", true)
  val fourHourTweetStream = groupAndFold(4, "4 hour", true)
  val twentyFourHourTweetStream = groupAndFold(6, "24 hour", true)

  val streamFinishedF = TwitterStream.twitterStream
    .via(TwitterStream.balancer(apiToTweetStream, 3))
    .via(fiveSecondTreatStream.async)
    .via(fifteenSecondTweetStream.async)
    .via(oneMinuteTweetStream.async)
    .via(fiveMinuteTweetStream.async)
    .via(tenMinuteTweetStream.async)
    .via(twentyMinuteTweetStream.async)
    .via(oneHourTweetStream.async)
    .via(fourHourTweetStream.async)
    .via(twentyFourHourTweetStream.async)
    .runWith(Sink.ignore)

  logger.info("awaiting stream termination")
  Await.result(streamFinishedF, Duration.Inf)
  logger.error("stream terminated")
  TwitterStream.shutdown()
  LogManager.shutdown()
}
