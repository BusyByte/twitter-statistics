package net.nomadicalien.twitter.models

import java.net.URL
import java.text.{ParsePosition, SimpleDateFormat}

import cats.Show
import cats.kernel.Monoid
import net.nomadicalien.twitter.service.TweetService


case class Emoji(short_name: String, text: Option[String])

sealed trait TwitterStatusApiModel

final case class HashTag(text: String)
final case class Url(expanded_url: Option[String])
final case class Media(`type`: String)
final case class Entities(
  hashtags: List[HashTag],
  urls: List[Url],
  media: Option[List[Media]]
)
final case class Tweet(created_at: String, entities: Entities, text: String) extends TwitterStatusApiModel

final case class Status(id: Long)
final case class Delete(status: Status)
final case class DeletedTweet(delete: Delete) extends TwitterStatusApiModel

final case class Warning(code: String, message: String, percent_full: Int)
final case class StreamWarning(warning: Warning) extends TwitterStatusApiModel


object Tweet {

  def decodeTwitterStatusApiModel(json: String): Either[ApplicationError, TwitterStatusApiModel] = {
    import net.nomadicalien.twitter.json.JsonDecoders._
    import io.circe.parser.decode
    import io.circe._, io.circe.parser._
    decode[TwitterStatusApiModel](json)
      .leftMap{e =>
        val prettyJson = parse(json).toOption.map(_.toString()).getOrElse("")
        TweetParseError(s"${e.getMessage}:Error parsing json:\n${prettyJson}\n" )
      }
      .toEither
  }

  val simpleDateFormatHolder = new ThreadLocal[SimpleDateFormat] {
    override def initialValue(): SimpleDateFormat = {
      new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy")
    }
  }

  def findTweetTime(tweet: Tweet): Long =
    simpleDateFormatHolder.get()
      .parse(tweet.created_at, new ParsePosition(0))
      .getTime

  def findHashTags(tweet: Tweet): Map[String, Int] = {
    tweet.entities.hashtags.map(_.text.toLowerCase)
      .foldLeft(Map.empty[String, Int]) {
        case (accMap, hashTag) =>
          val currentCount = accMap.getOrElse(hashTag, 0) + 1
          accMap.updated(hashTag, currentCount)
      }
  }
  def extractDomain(url: String): String = new URL(url).getHost
  def mergeCounts(left: Map[String, Int], right: Map[String, Int]): Map[String, Int] = {
    val keys = left.keySet ++ right.keySet
    keys.map {key =>
      val count = left.getOrElse(key, 0) + right.getOrElse(key, 0)
      (key, count)
    }.toMap
  }
  def minTime(left: Option[Long], right: Option[Long]): Option[Long] = {
    for {
      leftValue <- left.orElse(right)
      rightValue <- right.orElse(left)
    } yield math.min(leftValue, rightValue)
  }
  def maxTime(left: Option[Long], right: Option[Long]): Option[Long] =
    for {
      leftValue <- left.orElse(right)
      rightValue <- right.orElse(left)
    } yield math.max(leftValue, rightValue)
}
trait Conversion[A, B] {
  def convert(a: A): Either[ApplicationError, B]
}


/**
  * - total number of tweets received
  * - average tweets per hour/minute/second
  * - top 10 emojis
  * - percent of tweets w/ an emoji
  * - top 10 hashtags
  * - percent of tweets w/ a url
  * - percent of tweets w/ photo url
  * - top 10 domains of urls in tweets
  */
final case class Statistics(
  startTime: Option[Long],
  endTime: Option[Long],
  count: Int,
  emojis: Map[String, Int],
  emojiCount: Int,
  hashTags: Map[String, Int],
  urlCount: Int,
  photoUrlCount: Int,
  domains: Map[String, Int]
)

object Statistics {
  lazy val tweetService: TweetService = TweetService

  def top(n: Int)(counts: Map[String, Int]): Map[String, Int] = {
    counts.toList.sortBy(-_._2).take(n).toMap
  }

  val top10 = top(10)(_)
  val top1000 = top(1000)(_)

  lazy val showCounts = new Show[Map[String, Int]] {
    def show(counts: Map[String, Int]): String = {
      counts.toList.sortBy(-_._2).foldLeft("\n") {
        case (acc, (k, v)) =>
          acc + s"$v - $k\n"
      }
    }
  }

  object Implicits {

    implicit lazy val showStatistic = new Show[Statistics] {
      def show(stats: Statistics): String = {
        val maybeTweetRate = for {
          start <- stats.startTime
          end <- stats.endTime
        } yield {
          val elapsedMillis = end - start
          val elapsedSeconds = elapsedMillis / 1000
          val numTweets = stats.count
          val tweetsPerSecond = numTweets / math.max(elapsedSeconds, 1)
          val tweetsPerMinute = tweetsPerSecond * 60
          val tweetsPerHour =   tweetsPerMinute * 60
          (tweetsPerSecond, tweetsPerMinute, tweetsPerHour)
        }

        val tweetsPerSec = maybeTweetRate.map(_._1).getOrElse("")
        val tweetsPerMin = maybeTweetRate.map(_._2).getOrElse("")
        val tweetsPerHour = maybeTweetRate.map(_._3) .getOrElse("")

        s"""
          |Tweets/sec      = $tweetsPerSec
          |Tweets/min      = $tweetsPerMin
          |Tweets/hour     = $tweetsPerHour
          |Tweet Count     = ${stats.count}
          |Emoji Count     = ${stats.emojiCount}
          |Top 10 Emojis: ${showCounts.show(top10(stats.emojis))}
          |Top 10 Hash Tags: ${showCounts.show(top10(stats.hashTags))}
          |Photo URL Count = ${stats.photoUrlCount}
          |URL Count       = ${stats.urlCount}
          |Top 10 domains: ${showCounts.show(top10(stats.domains))}
          |Start Time      = ${stats.startTime.getOrElse("")}
          |End Time        = ${stats.endTime.getOrElse("")}
          |
        """.stripMargin
      }


    }

    implicit lazy val tweetToStats = new Conversion[Tweet, Statistics] {
      import cats.implicits._
      def convert(tweet: Tweet):Either[ApplicationError, Statistics] = {
        for {
          emojisCountMap <- tweetService.findEmojisCount(tweet)
        } yield {
          val currentTime = Tweet.findTweetTime(tweet)
          val emojiCount = emojisCountMap.values.sum
          val hashTags = Tweet.findHashTags(tweet)
          val urls = tweet.entities.urls.flatMap(_.expanded_url.toList)
          val photos = tweet.entities.media.toList.flatten.filter {_.`type` == "photo"}
          val domains = urls
            .map(Tweet.extractDomain)
            .map(_.toLowerCase)
            .groupBy(dom => dom)
            .map(p => (p._1, p._2.size))
          Statistics(
          startTime = Some(currentTime),
          endTime = Some(currentTime),
          count = 1,
          emojis = emojisCountMap,
          emojiCount = emojiCount,
          hashTags = hashTags,
          urlCount = urls.size,
          photoUrlCount = photos.size,
          domains = domains
        )
      }
    }
  }
    implicit lazy val statisticsMonoid = new Monoid[Statistics] {
      def empty: Statistics = {
        Statistics(
          startTime = None,
          endTime = None,
          count = 0,
          emojis = Map.empty[String, Int],
          emojiCount = 0,
          hashTags = Map.empty[String, Int],
          urlCount = 0,
          photoUrlCount = 0,
          domains = Map.empty[String, Int]
        )
      }

      def combine(x: Statistics, y: Statistics): Statistics =
        Statistics(
          startTime = Tweet.minTime(x.startTime, y.startTime),
          endTime = Tweet.maxTime(x.endTime, y.endTime),
          count = x.count + y.count,
          emojis = top1000(Tweet.mergeCounts(x.emojis, y.emojis)),
          emojiCount = x.emojiCount + y.emojiCount,
          hashTags = top1000(Tweet.mergeCounts(x.hashTags, y.hashTags)),
          urlCount = x.urlCount + y.urlCount,
          photoUrlCount = x.photoUrlCount + y.photoUrlCount,
          domains = top1000(Tweet.mergeCounts(x.domains, y.domains))
        )
    }
  }
}


sealed trait ApplicationError {
  def message: String
}
case class MissingConfigError(message: String) extends ApplicationError
case class HttpError(message: String) extends ApplicationError
case class TweetParseError(message: String) extends ApplicationError
case class EmojiParseError(message: String) extends ApplicationError
case class IOError(message: String) extends ApplicationError