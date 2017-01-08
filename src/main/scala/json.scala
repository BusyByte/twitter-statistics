package net.nomadicalien.twitter.json

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import net.nomadicalien.twitter.models._

trait JsonDecoders {
  implicit val emojiDecoder: Decoder[Emoji] = deriveDecoder[Emoji]
  implicit val warningDecoder: Decoder[Warning] = deriveDecoder[Warning]
  implicit val mediaDecoder: Decoder[Media] = deriveDecoder[Media]
  implicit val urlDecoder: Decoder[Url] = deriveDecoder[Url]
  implicit val hashTagDecoder: Decoder[HashTag] = deriveDecoder[HashTag]
  implicit val entititesDecoder: Decoder[Entities] = deriveDecoder[Entities]

  implicit val statusDecoder: Decoder[Status] = deriveDecoder[Status]
  implicit val deleteDecoder: Decoder[Delete] = deriveDecoder[Delete]

  implicit val streamWarningDecoder: Decoder[StreamWarning] = deriveDecoder[StreamWarning]
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]
  implicit val deletedTweetDecoder: Decoder[DeletedTweet] = deriveDecoder[DeletedTweet]


  implicit val twitterStatusApiModelDecoder: Decoder[TwitterStatusApiModel] =
    Decoder[Tweet].map[TwitterStatusApiModel](identity)
      .or(
        Decoder[DeletedTweet].map[TwitterStatusApiModel](identity)
            .or(Decoder[StreamWarning].map[TwitterStatusApiModel](identity))

      )

}

object JsonDecoders extends JsonDecoders
