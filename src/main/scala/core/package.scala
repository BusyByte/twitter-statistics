import scala.collection.mutable

/**
 * Created by Shawn on 11/9/2014.
 */
package object core {

  case object PrintReport
  case class Report(
                     totalTweets: String,
                     tweetsInTimeframe: String,
                     topEmojis: String,
                     tweetsWithEmoji: String,
                     topHashTags: String,
                     tweetsWithUrl: String,
                     tweetsWithPhotoUrl: String,
                     topDomains: String) {
    override def toString: String = s"""
        |$totalTweets
        |$tweetsInTimeframe
        |$topEmojis
        |$tweetsWithEmoji
        |$topHashTags
        |$tweetsWithUrl
        |$tweetsWithPhotoUrl
        |$topDomains
        """.stripMargin
  }

  sealed trait ReportEvent
  case class TweetCount(count: Int) extends ReportEvent
  case class EmojiCount(count: Int) extends ReportEvent
  case class TopEmojis(summaryText: String) extends ReportEvent
  case class TopHashTags(summaryText: String) extends ReportEvent
  case class UrlCount(count: Int) extends ReportEvent
  case class PhotoCount(count: Int) extends ReportEvent
  case class TopDomains(summaryText: String) extends ReportEvent

  val topCount = 10

  def formatCountMap(theMap: mutable.Map[String, Int]): String = {
    theMap.toList.sortBy(-_._2).take(topCount).map(pair => s"""{\"${pair._1}\" : \"${pair._2}\"}""").mkString("[", ",", "]")
  }
}
