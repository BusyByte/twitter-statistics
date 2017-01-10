package net.nomadicalien.twitter

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink}
import cats.Show
import cats.kernel.Monoid
import net.nomadicalien.twitter.actor.ActorModule
import net.nomadicalien.twitter.models._
import net.nomadicalien.twitter.repository.TweetRepository
import net.nomadicalien.twitter.stream.StreamUtils
import org.apache.logging.log4j.{LogManager, Logger}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt

trait Program extends StreamUtils with StreamLoggingHelper {
  implicit def tweetToStats: Conversion[Tweet, Statistics]
  implicit def statisticsMonoid: Monoid[Statistics]
  implicit def showStatistic: Show[Statistics]
  implicit def materializer: ActorMaterializer

  def tweetRepository: TweetRepository
  implicit def logger: Logger

  val apiToTweetStream = Flow[Either[ApplicationError, TweetStatus]]
    .map(logTweetErrors)
    .collect {
      case Right(t: Tweet) => tweetToStats.convert(t)
    }
    .map(logStatisticsErrors)
    .collect {
      case Right(s : Statistics) => s
    }

  val fiveSecondTreatStream =
    Flow[Statistics]
      .groupedWithin(Int.MaxValue, 5.seconds)
      .map{ groupedStats: Seq[Statistics] =>
        val results = groupedStats.foldLeft(statisticsMonoid.empty)(statisticsMonoid.combine)
        results
      }

  def groupAndFold(groupSize: Int, context: String, logStats: Boolean): Flow[Statistics, Statistics, NotUsed] =
    Flow[Statistics]
      .grouped(groupSize)
      .map { groupedStats =>
        val results = groupedStats.foldLeft(statisticsMonoid.empty)(statisticsMonoid.combine)
        if(logStats) {
          logger.info(s"Current $context tweet count ${showStatistic.show(results)}")
        }
        results
      }.async

  val fifteenSecondTweetStream = groupAndFold(3, "15 second", false)
  val oneMinuteTweetStream = groupAndFold(4, "1 minute", false)
  val fiveMinuteTweetStream = groupAndFold(5, "5 minute", true)
  val tenMinuteTweetStream = groupAndFold(2, "10 minute", true)
  val twentyMinuteTweetStream = groupAndFold(2, "20 minute", true)
  val oneHourTweetStream = groupAndFold(3, "1 hour", true)
  val fourHourTweetStream = groupAndFold(4, "4 hour", true)
  val twentyFourHourTweetStream = groupAndFold(6, "24 hour", true)

  val foldingSink: Sink[Statistics, Future[Statistics]] = Sink.
    fold(statisticsMonoid.empty)(statisticsMonoid.combine)
    .async

  def graph: RunnableGraph[Future[Statistics]] = tweetRepository.tweetSampleStream
    .via(balancer(apiToTweetStream, 3))
    .via(fiveSecondTreatStream)
    .via(fifteenSecondTweetStream)
    .via(oneMinuteTweetStream)
    .via(fiveMinuteTweetStream)
    .via(tenMinuteTweetStream)
    .via(twentyMinuteTweetStream)
    .via(oneHourTweetStream)
    .via(fourHourTweetStream)
    .via(twentyFourHourTweetStream)
    .toMat(foldingSink)(Keep.right)

  def run: Unit = {
    logger.info("starting")

    val resultStatistics: Future[Statistics] = graph.run()

    logger.info("awaiting stream termination")
    val resultingStats: Statistics = Await.result(resultStatistics, Duration.Inf)
    logger.error("stream terminated")
    logger.info(s"Resulting stats ${showStatistic.show(resultingStats)}")
  }
}

object Program extends Program {
  import net.nomadicalien.twitter.models.Statistics

  implicit lazy val tweetToStats: Conversion[Tweet, Statistics] = Statistics.Implicits.tweetToStats
  implicit lazy val statisticsMonoid: Monoid[Statistics] = Statistics.Implicits.statisticsMonoid
  implicit lazy val showStatistic: Show[Statistics] = Statistics.Implicits.showStatistic

  implicit lazy val materializer: ActorMaterializer = ActorModule.materializer

  lazy val tweetRepository: TweetRepository = TweetRepository

  implicit lazy val logger: Logger = LogManager.getLogger(this.getClass)
}


trait StreamLoggingHelper {
  def logTweetErrors(maybeTweet: Either[ApplicationError, TweetStatus])(implicit logger: Logger): Either[ApplicationError, TweetStatus] = maybeTweet match {
    case Right(StreamWarning(warning)) =>
      logger.warn("Warning: " + warning)
      maybeTweet
    case Left(error) =>
      logger.error("Error: " + error.message)
      maybeTweet
    case _ => maybeTweet
  }

  def logStatisticsErrors(maybeStatistics: Either[ApplicationError, Statistics])(implicit logger: Logger): Either[ApplicationError, Statistics] = maybeStatistics match {
    case Left(error) =>
      logger.error("Error: " + error.message)
      maybeStatistics
    case _ => maybeStatistics
  }
}

object StreamLoggingHelper extends StreamLoggingHelper