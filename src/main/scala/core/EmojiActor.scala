package core

import java.io.{InputStreamReader}

import akka.actor.Actor
import domain._
import spray.json.{JsString, JsObject, JsArray, JsonParser}

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

  def findEmojis(tweet: Tweet): List[Emoji] = {
    emojis.filter(emoji => tweet.text.contains(emoji.text.get))
  }

  def updateEmojiStats(tweet: Tweet) : Unit = {
    val emojis = findEmojis(tweet)
    if(emojis.nonEmpty) {
      incrementEmojiTweetCount()
      emojis.foreach {
        emoji =>
          val emojiName = emoji.name
          val count = emojiCounts(emojiName)
          emojiCounts.update(emojiName, count + 1)
      }

      context.system.eventStream.publish(TopEmojis(topEmojisInTweets))
      context.system.eventStream.publish(EmojiCount(numberOfEmojiTweets))
    }
  }

  def incrementEmojiTweetCount(): Unit = {
    numberOfEmojiTweets = numberOfEmojiTweets + 1
  }

  def topEmojisInTweets: String = formatCountMap(emojiCounts)




}
