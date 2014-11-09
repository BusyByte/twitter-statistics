package core

import java.net.URL

import akka.actor.{Props, Actor}
import domain.{HashTagText, EmojiName, Emojis, Tweet}
import scala.collection.mutable

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


  override def receive: Receive = {
    case tweet: Tweet => updateTweetStats(tweet)
    case emojis: Emojis => updateEmojiStats(emojis)
  }

  def updateTweetStats(tweet: Tweet): Unit = {
    emojiProcessor ! FindEmojis(tweet)
    incrementTweetCount()
    updateHashTagCounts(tweet)
    updateUrlCounts(tweet)
    updatePhotoCounts(tweet)
  }

  def printReport(): Unit = {
    //Total number of tweets received

    //Average tweets per hour/minute/second

    //Top emojis in tweets

    //Percent of tweets that contains emojis

    //Top hashtags

    //Percent of tweets that contain a url

    //Percent of tweets that contain a photo url (pic.twitter.com or instagram)

    //Top domains of urls in tweets
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
        val hashTagText = tag.text
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


}
