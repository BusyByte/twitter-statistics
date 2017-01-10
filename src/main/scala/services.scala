package net.nomadicalien.twitter.service

import net.nomadicalien.twitter.models.{ApplicationError, Tweet}
import net.nomadicalien.twitter.repository.{ClassPathEmojiRepository, EmojiRepository}

import scala.annotation.tailrec

trait TweetService {
  def findEmojisCount(tweet: Tweet): Either[ApplicationError, Map[String, Int]]
}


private[service] trait TweetServiceInterpreter extends TweetService {
  import cats.implicits._
  def emojiRepository: EmojiRepository
  def findEmojisCount(tweet: Tweet): Either[ApplicationError, Map[String, Int]] = {
    val tweetText = tweet.text
    for {
      textEmojis <- emojiRepository.getTextEmojis
    } yield {
      textEmojis.foldLeft(Map.empty[String, Int]) {
        case (emojiMap, emoji) =>
          val shortName = emoji.short_name
          val emojiCount = emoji.text.map(findEmojiCount(tweetText, _)).getOrElse(0)
          if(emojiCount > 0)
            emojiMap.updated(shortName, emojiCount)
          else
            emojiMap
      }
    }
  }

  def findEmojiCount(text: String, emoji: String): Int = {
    val emojiLength = emoji.length
    val textLength = text.length

    @tailrec def recFindEmojiCount(startIndex: Int, count: Int): Int = {
      if(startIndex >= textLength) {
        count
      } else {
        val foundIndex = text.indexOf(emoji, startIndex)
        if (foundIndex == -1) {
          count
        } else {
          recFindEmojiCount(foundIndex + emojiLength, count + 1)
        }
      }
    }

    recFindEmojiCount(0, 0)
  }
}

object TweetService extends TweetServiceInterpreter {
  lazy val emojiRepository: EmojiRepository = ClassPathEmojiRepository
}


