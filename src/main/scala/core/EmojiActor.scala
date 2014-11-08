package core

import java.io.{InputStreamReader}

import akka.actor.Actor
import domain.{Emoji, Emojis, Tweet}
import spray.json.{JsString, JsObject, JsArray, JsonParser}

/**
 * Created by Shawn on 11/8/2014.
 */
class EmojiActor extends Actor {

  val json = JsonParser(scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("emoji_pretty.json")).mkString)
  val emojisWithText: List[Emoji] = (json match {
    case JsArray(elements) if elements.nonEmpty => elements.map(jsValue => mkEmoji(jsValue.asJsObject))
  }).filter(_.text.isDefined)//TODO: possibly just not collect ones without text to start with rather than filter out afterwards
  
  def mkEmoji(emoji: JsObject): Emoji = {
    val nameString: String = emoji.fields.get("name").get match {
      case JsString(name) => name
    }
    val maybeTextString: Option[String] = emoji.fields.get("text").collect {
      case JsString(text) => text
    }

    Emoji(nameString, maybeTextString)
  }

  override def receive: Receive = {
    case FindEmojis(tweet) => findEmojis(tweet)
  }

  def findEmojis(tweet: Tweet): Unit = {
    sender ! Emojis(emojisWithText.filter(emoji => tweet.text.contains(emoji.text.get)))

  }
}
