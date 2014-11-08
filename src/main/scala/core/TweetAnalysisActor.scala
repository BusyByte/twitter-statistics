package core

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

  val emojiProcessor = context.system.actorOf(Props[EmojiActor])

  val emojiCounts = mutable.Map[EmojiName, Int]().withDefaultValue(0)
  val hashTagCounts = mutable.Map[HashTagText, Int]().withDefaultValue(0)

  override def receive: Receive = {
    case tweet: Tweet => updateTweetStats(tweet)
    case emojis: Emojis => updateEmojiStats(emojis)

  }

  def updateTweetStats(tweet: Tweet): Unit = {
    emojiProcessor ! FindEmojis(tweet)
    incrementTweetCount()
    updateHashTagCounts(tweet)
  }

  def updateHashTagCounts(tweet: Tweet): Unit = {
    tweet.hashtags.foreach {
      tag =>
        val hashTagText = tag.text
        val count = hashTagCounts(hashTagText)
        hashTagCounts.update(hashTagText, count)
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
