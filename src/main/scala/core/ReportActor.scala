package core

import akka.actor.Actor

/**
 * Created by Shawn on 11/9/2014.
 */
class ReportActor extends Actor {

  val startTime: Long = System.currentTimeMillis()
  var tweetCount: Option[TweetCount] = None
  var topEmojis: Option[TopEmojis] = None
  var emojiCount: Option[EmojiCount] = None
  var topHashTags: Option[TopHashTags] = None
  var urlCount: Option[UrlCount] = None
  var photoCount: Option[PhotoCount] = None
  var topDomains: Option[TopDomains] = None

  context.system.eventStream.subscribe(self, classOf[ReportEvent])

  override def receive: Receive = {
    case tc: TweetCount => tweetCount = Some(tc)
    case te: TopEmojis => topEmojis = Some(te)
    case ec: EmojiCount => emojiCount = Some(ec)
    case tht: TopHashTags => topHashTags = Some(tht)
    case uc: UrlCount => urlCount = Some(uc)
    case pc: PhotoCount => photoCount = Some(pc)
    case td: TopDomains => topDomains = Some(td)
    case PrintReport => printReport()
  }

  def printReport(): Unit = {
    val report = Report(
      totalTweets = s"total number of tweets received: ${tweetCount.map(_.count).getOrElse(0)}",
      tweetsInTimeframe = s"average tweets per hour/minute/second: $tweetsPerHour/$tweetsPerMinute/$tweetsPerSecond",
      topEmojis = s"top $topCount emojis: ${topEmojis.map(_.summaryText).getOrElse("")}",
      tweetsWithEmoji = s"tweets w/ an emoji: ${percentEmojis}%",
      topHashTags = s"top $topCount hashtags: ${topHashTags.map(_.summaryText).getOrElse("")}",
      tweetsWithUrl = s"tweets w/ a url: ${percentUrls}%",
      tweetsWithPhotoUrl = s"tweets w/ photo url: ${percentPhotos}%",
      topDomains = s"top $topCount domains of urls in tweets: ${topDomains.map(_.summaryText).getOrElse("")}"
    )

    println(report.toString)
    context.system.eventStream.publish(report)
  }

  def tweetsPerSecond: Long = {
    val elapsedSeconds: Long = elapsedTime / 1000

    (tweetCount, elapsedSeconds) match {
      case (Some(tCount), elapSecs) if elapSecs > 0 => tCount.count / elapSecs
      case _ => 0l
    }
  }

  def elapsedTime: Long = System.currentTimeMillis() - startTime

  def tweetsPerMinute: Long = tweetsPerSecond * 60
  def tweetsPerHour: Long = tweetsPerMinute * 60

  def percentEmojis = {
    tweetCount.map { numTweets =>
      emojiCount.map(_.count * 100 / numTweets.count).getOrElse(0)
    }.getOrElse(0)
  }
  def percentUrls = {
    tweetCount.map { numTweets =>
      urlCount.map(_.count * 100 / numTweets.count).getOrElse(0)
    }.getOrElse(0)
  }
  def percentPhotos = {
    tweetCount.map { numTweets =>
      photoCount.map(_.count * 100 / numTweets.count).getOrElse(0)
    }.getOrElse(0)
  }
}
