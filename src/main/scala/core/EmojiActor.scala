package core

import java.io.{InputStreamReader}

import akka.actor.Actor
import domain._
import spray.json.{JsString, JsObject, JsArray, JsonParser}

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Created by Shawn on 11/8/2014.
 */
class EmojiActor extends Actor {

  val emojis: List[Emoji] = new EmojiLoader().loadEmojis(EmojiLoader.json)
  var numberOfEmojiTweets: Int = 0
  val emojiCounts = mutable.Map[EmojiName, Int]().withDefaultValue(0)

  override def receive: Receive = {
    case tweet: Tweet => updateEmojiStats(tweet)
  }

  def findEmojis(tweet: Tweet): List[(Emoji,Int)] =
    emojis.map {
      e =>
        (e, countEmojiInText(e,tweet))
    }.filter(_._2 > 0)


  def updateEmojiStats(tweet: Tweet) : Unit = {
    val emojis = findEmojis(tweet)
    if(emojis.nonEmpty) {
      incrementEmojiTweetCount()
      emojis.foreach {
        e =>
          val emojiName = e._1.name
          val emojiCount = e._2
          val count = emojiCounts(emojiName)
          emojiCounts.update(emojiName, count + emojiCount)
      }

      context.system.eventStream.publish(TopEmojis(topEmojisInTweets))
      context.system.eventStream.publish(EmojiCount(numberOfEmojiTweets))
    }
  }

  @tailrec
  private def countEmojiInText(emoji: Emoji, tweet: Tweet, count:Int = 0, startingIndex:Int = 0): Int = {
    val tweetText: String = tweet.text
    if(startingIndex >= tweetText.length) {
      count
    } else {
      val emojiText: String = emoji.text
      val nextIndex = tweetText.indexOf(emojiText, startingIndex)
      if(nextIndex == -1) {
        count
      } else {
        countEmojiInText(emoji, tweet, count + 1, nextIndex + emojiText.length)
      }
    }
  }


  def incrementEmojiTweetCount(): Unit = {
    numberOfEmojiTweets = numberOfEmojiTweets + 1
  }

  def topEmojisInTweets: String = formatCountMap(emojiCounts)




}
