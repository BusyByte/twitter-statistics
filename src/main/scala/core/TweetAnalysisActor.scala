package core

import akka.actor.{Props, Actor}
import domain.{Emojis, Tweet}

/**
 * Created by Shawn on 11/8/2014.
 */
class TweetAnalysisActor extends Actor {
  val startupTime = System.currentTimeMillis()
  var numberOfTweets: Int = 0
  var numberOfEmojiTweets: Int = 0

  val emojiProcessor = context.system.actorOf(Props[EmojiActor])


  override def receive: Receive = {
    case tweet: Tweet => updateTweetStats(tweet)
    case emojis: Emojis => updateEmojiStats(emojis)

  }

  def updateTweetStats(tweet: Tweet): Unit = {
    emojiProcessor ! FindEmojis(tweet)
    incrementTweetCount()
  }

  def incrementTweetCount(): Unit = {
    numberOfTweets = numberOfTweets + 1
  }

  def updateEmojiStats(emojis: Emojis) : Unit = {
    if(emojis.emojis.nonEmpty) {
      incrementEmojiTweetCount()
    }
  }
  
  def incrementEmojiTweetCount(): Unit = {
    numberOfEmojiTweets = numberOfEmojiTweets + 1
  }


}
