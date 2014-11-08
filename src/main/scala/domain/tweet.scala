package domain

case class User(id: String, lang: String, followersCount: Int)

case class Place(country: String, name: String) {
  override lazy val toString = s"$name, $country"
}

case class Tweet(id: String, user: User, text: String, place: Option[Place], urls: List[Url], hashtags: List[HashTag], photos: List[Photo])

case class Url(expandedUrl: String)

case class HashTag(text: HashTagText)

case class Photo(displayUrl: String)

case class Emoji(name: EmojiName, text: Option[String])

case class Emojis(emojis: List[Emoji])