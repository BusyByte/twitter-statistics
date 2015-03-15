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
    val emojisWithText: List[Emoji] = jsonValue match {
        case JsArray(Nil) => Nil
        case JsArray(elements) => elements.flatMap(jsValue => mkEmoji(jsValue.asJsObject))
      }
    emojisWithText
  }

  private def mkEmoji(emoji: JsObject): Option[Emoji] = {
    val nameString: String = emoji.fields.get("name").get match {
      case JsString(name) => name
    }
    val maybeTextString: Option[String] = emoji.fields.get("text").collect {
      case JsString(text) => text
    }

    maybeTextString.map {
      text =>
        Emoji(nameString, text)
    }
  }
}
