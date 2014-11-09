package core

import java.net.URL

import akka.actor.{Props, Actor}
import domain.{HashTagText, EmojiName, Emojis, Tweet}
import scala.collection.mutable
import scala.concurrent.duration._

/**
 * Created by Shawn on 11/8/2014.
 */
class TweetAnalysisActor extends Actor {
  val startupTime = System.currentTimeMillis()
  var numberOfTweets: Int = 0
  var numberOfEmojiTweets: Int = 0
  var numberOfUrlTweets: Int = 0
  var numberOfPhotoTweets: Int = 0

  val emojiProcessor = context.system.actorOf(Props[EmojiActor])

  val emojiCounts = mutable.Map[EmojiName, Int]().withDefaultValue(0)
  val hashTagCounts = mutable.Map[HashTagText, Int]().withDefaultValue(0)
  val domainCounts = mutable.Map[String, Int]().withDefaultValue(0)
  val topCount = 10

  case object PrintReport

  context.system.scheduler.schedule(10 seconds, 10 seconds, self, PrintReport)(context.dispatcher)

  def formatCountMap(theMap: mutable.Map[String, Int]): String = {
    theMap.toList.sortBy(-_._2).take(topCount).map(pair => s"""{\"${pair._1}\" : \"${pair._2}\"}""").mkString("[", ",", "]")
  }

  def topEmojisInTweets: String = formatCountMap(emojiCounts)
  
  def topHashTagsInTweets: String = formatCountMap(hashTagCounts)

  def topDomainsInTweets: String = formatCountMap(domainCounts)

  override def receive: Receive = {
    case tweet: Tweet => updateTweetStats(tweet)
    case emojis: Emojis => updateEmojiStats(emojis)
    case PrintReport => printReport()
  }

  def updateTweetStats(tweet: Tweet): Unit = {
    emojiProcessor ! FindEmojis(tweet)
    incrementTweetCount()
    updateHashTagCounts(tweet)
    updateUrlCounts(tweet)
    updatePhotoCounts(tweet)
  }

  def percentEmojis = numberOfEmojiTweets * 100 / numberOfTweets
  def percentUrls = numberOfUrlTweets * 100 / numberOfTweets
  def percentPhotos = numberOfPhotoTweets * 100 / numberOfTweets

  def printReport(): Unit = {
    if(numberOfTweets > 0) {
      println( s"""
         |total number of tweets received: $numberOfTweets
         |average tweets per hour/minute/second: $tweetsPerHour/$tweetsPerMinute/$tweetsPerSecond
         |top $topCount emojis: $topEmojisInTweets
         |tweets w/ an emoji: ${percentEmojis}%
         |top $topCount hashtags: $topHashTagsInTweets
         |tweets w/ a url: ${percentUrls}%
         |tweets w/ photo url: ${percentPhotos}%
         |top $topCount domains of urls in tweets: $topDomainsInTweets
       """.stripMargin)
    }
  }

  def updatePhotoCounts(tweet: Tweet): Unit = {
      val countOfPhotos = tweet.photos.foldLeft(0) {
        (count, photo) =>
          if(photo.displayUrl.toLowerCase.contains("pic.twitter.com") || photo.displayUrl.toLowerCase.contains("instagram")) {
            count + 1
          } else {
            count
          }
      }

     numberOfPhotoTweets = numberOfPhotoTweets + countOfPhotos
  }

  def updateUrlCounts(tweet: Tweet): Unit = {
    if(tweet.urls.nonEmpty) {
      incrementUrlCounts()
      tweet.urls.foreach {
        tweetUrl =>
        val theUrl = new URL(tweetUrl.expandedUrl)
        val domain = theUrl.getHost.toLowerCase
        val count = domainCounts(domain)
        domainCounts.update(domain, count + 1)
      }
    }
  }

  def incrementUrlCounts(): Unit = {
    numberOfUrlTweets = numberOfUrlTweets + 1
  }

  def updateHashTagCounts(tweet: Tweet): Unit = {
    tweet.hashtags.foreach {
      tag =>
        val hashTagText = tag.text.toLowerCase
        val count = hashTagCounts(hashTagText)
        hashTagCounts.update(hashTagText, count + 1)
    }
  }

  def incrementTweetCount(): Unit = {
    numberOfTweets = numberOfTweets + 1
  }

  def updateEmojiStats(emojis: Emojis) : Unit = {
    if(emojis.emojis.nonEmpty) {
      incrementEmojiTweetCount()
      emojis.emojis.foreach {
        emoji =>
          val emojiName = emoji.name
          val count = emojiCounts(emojiName)
          emojiCounts.update(emojiName, count + 1)
      }
    }
  }
  
  def incrementEmojiTweetCount(): Unit = {
    numberOfEmojiTweets = numberOfEmojiTweets + 1
  }

  def tweetsPerSecond: Long = {
    val elapsedMillis = System.currentTimeMillis() - startupTime
    val elapsedSeconds = elapsedMillis / 1000
    numberOfTweets / elapsedSeconds
  }

  def tweetsPerMinute: Long = tweetsPerSecond * 60

  def tweetsPerHour: Long = tweetsPerMinute * 60


}
