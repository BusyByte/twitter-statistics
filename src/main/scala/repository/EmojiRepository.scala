package net.nomadicalien.twitter.repository

import net.nomadicalien.twitter.json.JsonDecoders
import net.nomadicalien.twitter.models._

import scala.io.Source
import scala.util.{Failure, Success, Try}

trait EmojiRepository {
  def getTextEmojis: Either[ApplicationError, List[Emoji]]
}

import io.circe.parser.decode
import cats.implicits._
private[repository] trait ClassPathEmojiRepository extends EmojiRepository with JsonDecoders {

  def emojiSource: Either[ApplicationError, Source] = {
    val source = Try {
      Source.fromInputStream(getClass.getResourceAsStream("/emoji_pretty.json"))
    }
    source match {
      case Success(s) => Right(s)
      case Failure(e) => Left(IOError(e.getMessage))
    }
  }

  def getTextEmojis: Either[ApplicationError, List[Emoji]] = {
    for {
      emojisString <- readEmojiText()
      emojies <- parseEmojis(emojisString)
    } yield {
      emojies
        .foldLeft(Map.empty[String, Emoji]) {
          case (emojiMap, emoji@Emoji(_, Some(text))) =>
            emojiMap.updated(text, emoji)
          case (emojiMap, _) => emojiMap
        }.values.toList
    }
  }

  def parseEmojis(emojisString: String): Either[ApplicationError, List[Emoji]] = {
    decode[List[Emoji]](emojisString)
      .leftMap(e => EmojiParseError(e.getMessage))
      .toEither
  }

  def readEmojiText(): Either[ApplicationError, String] = {
    val emojiText = for {
      source <- emojiSource
      fullText <- Try(source.mkString) match {
        case Success(s) => Right(s)
        case Failure(e) => Left(IOError(e.getMessage))
      }
    } yield fullText

    Try(emojiSource.toOption.foreach(_.close()))

    emojiText
  }
}

object ClassPathEmojiRepository extends ClassPathEmojiRepository {
  override lazy val getTextEmojis: Either[ApplicationError, List[Emoji]]
  = super.getTextEmojis
}
