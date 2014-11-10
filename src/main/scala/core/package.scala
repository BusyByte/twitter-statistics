import scala.collection.mutable

/**
 * Created by Shawn on 11/9/2014.
 */
package object core {

  case object PrintReport

  case class TweetCount(count: Int)
  case class ElapsedTime(time: Long)
  case class EmojiCount(count: Int)
  case class TopEmojis(summaryText: String)
  case class TopHashTags(summaryText: String)
  case class UrlCount(count: Int)
  case class PhotoCount(count: Int)
  case class TopDomains(summaryText: String)

  val topCount = 10

  def formatCountMap(theMap: mutable.Map[String, Int]): String = {
    theMap.toList.sortBy(-_._2).take(topCount).map(pair => s"""{\"${pair._1}\" : \"${pair._2}\"}""").mkString("[", ",", "]")
  }
}
