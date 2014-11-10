package core

import domain.Emoji
import spray.json.{JsString, JsObject, JsArray, JsonParser}

object EmojiLoader {
  val json = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("emoji_pretty.json")).mkString
}

/**
 * Created by Shawn on 11/9/2014.
 */
class EmojiLoader {

  def loadEmojis(json: String): List[Emoji] = {
    val jsonValue = JsonParser(json)
    val emojisWithText: List[Emoji] = (jsonValue match {
      case JsArray(elements) if elements.nonEmpty => elements.map(jsValue => mkEmoji(jsValue.asJsObject))
    }).filter(_.text.isDefined)
    emojisWithText
  }

  private def mkEmoji(emoji: JsObject): Emoji = {
    val nameString: String = emoji.fields.get("name").get match {
      case JsString(name) => name
    }
    val maybeTextString: Option[String] = emoji.fields.get("text").collect {
      case JsString(text) => text
    }

    Emoji(nameString, maybeTextString)
  }
}
