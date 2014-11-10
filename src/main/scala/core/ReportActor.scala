package core

import akka.actor.Actor

/**
 * Created by Shawn on 11/9/2014.
 */
class ReportActor extends Actor {

  var tweetCount: Option[TweetCount] = None
  var elapsedTime: Option[ElapsedTime] = None
  var topEmojis: Option[TopEmojis] = None
  var emojiCount: Option[EmojiCount] = None
  var topHashTags: Option[TopHashTags] = None
  var urlCount: Option[UrlCount] = None
  var photoCount: Option[PhotoCount] = None
  var topDomains: Option[TopDomains] = None

  context.system.eventStream.subscribe(self, classOf[TweetCount])
  context.system.eventStream.subscribe(self, classOf[ElapsedTime])
  context.system.eventStream.subscribe(self, classOf[TopEmojis])
  context.system.eventStream.subscribe(self, classOf[EmojiCount])
  context.system.eventStream.subscribe(self, classOf[TopHashTags])
  context.system.eventStream.subscribe(self, classOf[UrlCount])
  context.system.eventStream.subscribe(self, classOf[PhotoCount])
  context.system.eventStream.subscribe(self, classOf[TopDomains])

  override def receive: Receive = {
    case tc: TweetCount => tweetCount = Some(tc)
    case et: ElapsedTime => elapsedTime = Some(et)
    case te: TopEmojis => topEmojis = Some(te)
    case ec: EmojiCount => emojiCount = Some(ec)
    case tht: TopHashTags => topHashTags = Some(tht)
    case uc: UrlCount => urlCount = Some(uc)
    case pc: PhotoCount => photoCount = Some(pc)
    case td: TopDomains => topDomains = Some(td)
    case PrintReport => printReport()
  }

  def printReport(): Unit = {
    if(tweetCount.isDefined && elapsedTime.isDefined && topEmojis.isDefined && emojiCount.isDefined && topHashTags.isDefined && urlCount.isDefined && photoCount.isDefined && topDomains.isDefined) {
      println( s"""
         |total number of tweets received: ${tweetCount.get.count}
         |average tweets per hour/minute/second: $tweetsPerHour/$tweetsPerMinute/$tweetsPerSecond
         |top $topCount emojis: ${topEmojis.get.summaryText}
         |tweets w/ an emoji: ${percentEmojis}%
         |top $topCount hashtags: ${topHashTags.get.summaryText}
         |tweets w/ a url: ${percentUrls}%
         |tweets w/ photo url: ${percentPhotos}%
         |top $topCount domains of urls in tweets: ${topDomains.get.summaryText}
       """.stripMargin)
    }
  }

  def tweetsPerSecond: Long = {
    val elapsedSeconds = elapsedTime.get.time / 1000
    tweetCount.get.count / elapsedSeconds
  }

  def tweetsPerMinute: Long = tweetsPerSecond * 60
  def tweetsPerHour: Long = tweetsPerMinute * 60

  def percentEmojis = emojiCount.get.count * 100 / tweetCount.get.count
  def percentUrls = urlCount.get.count * 100 / tweetCount.get.count
  def percentPhotos = photoCount.get.count * 100 / tweetCount.get.count
}
