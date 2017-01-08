package net.nomadicalien.twitter.repository

import net.nomadicalien.twitter.models._

import scala.io.Source
import scala.util.{Failure, Success, Try}

trait EmojiRepository {
  def textBasedEmojis: Either[ApplicationError, List[Emoji]]
}

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import cats.implicits._
private[repository] trait ClassPathEmojiRepository extends EmojiRepository {
  implicit val warningDecoder: Decoder[Emoji] = deriveDecoder[Emoji]

  def emojiSource: Either[ApplicationError, Source] = {
    val source = Try {
     Source.fromInputStream(getClass.getResourceAsStream("/emoji_pretty.json"))
    }
    source match {
      case Success(s) => Right(s)
      case Failure(e) => Left(IOError(e.getMessage))
    }
  }

  def textBasedEmojis: Either[ApplicationError, List[Emoji]] = {
    for {
      emojisString <- readEmojiText()
      emojies <- parseEmojis(emojisString)
    } yield emojies.filter(_.text.isDefined)
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
  override lazy val textBasedEmojis: Either[ApplicationError, List[Emoji]]
    = super.textBasedEmojis
}