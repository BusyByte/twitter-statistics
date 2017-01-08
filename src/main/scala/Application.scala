package net.nomadicalien.twitter

import akka.NotUsed
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
      case Left(error) =>
        logger.error("Error: " + error.message)
        false
      case _ => false
    }
    .collect {
      case Right(t: Tweet) => tweetToStats.convert(t)
    }

  val fiveSecondTreatStream = Flow[Statistics].groupedWithin(Int.MaxValue, 5.seconds)
    .map{ groupedStats: Seq[Statistics] =>
      val results = groupedStats.foldLeft(statisticsMonoid.empty)(statisticsMonoid.combine)
      logger.info(s"Current 5 second tweet count ${showStatistic.show(results)}")
      results
    }.async

  def groupAndFold(groupSize: Int, context: String) =
    Flow[Statistics]
      .grouped(groupSize)
      .map { groupedStats =>
        val results = groupedStats.foldLeft(statisticsMonoid.empty)(statisticsMonoid.combine)
        logger.info(s"Current $context tweet count ${showStatistic.show(results)}")
        results
      }.async

  val fifteenSecondTweetStream = groupAndFold(3, "15 second")
  val oneMinuteTweetStream = groupAndFold(4, "1 minute")
  val fifteenMinuteTweetStream = groupAndFold(15, "15 minute")
  val oneHourTweetStream = groupAndFold(4, "1 hour")
  val fourHourTweetStream = groupAndFold(4, "1 hour")
  val twentyFourHourTweetStream = groupAndFold(6, "24 hour")

  val streamFinishedF = TwitterStream.twitterStream
    .via(apiToTweetStream)
    .via(fiveSecondTreatStream)
    .via(fifteenSecondTweetStream)
    .via(oneMinuteTweetStream)
    .via(fifteenMinuteTweetStream)
    .via(oneHourTweetStream)
    .via(fourHourTweetStream)
    .via(twentyFourHourTweetStream)
    .runWith(Sink.ignore)

  logger.info("awaiting stream termination")
  Await.result(streamFinishedF, Duration.Inf)
  logger.error("stream terminated")
  TwitterStream.shutdown()
}
