package core

import akka.actor.Actor
import domain._

import scala.collection.mutable

/**
 * Created by Shawn on 11/9/2014.
 */
class HashTagActor extends Actor {

  val hashTagCounts = mutable.Map[HashTagText, Int]().withDefaultValue(0)

  override def receive: Receive = {
    case tweet: Tweet => updateHashTagCounts(tweet)
  }

  def updateHashTagCounts(tweet: Tweet): Unit = {
    if(tweet.hashtags.nonEmpty) {
      tweet.hashtags.foreach {
        tag =>
          val hashTagText = tag.text.toLowerCase
          val count = hashTagCounts(hashTagText)
          hashTagCounts.update(hashTagText, count + 1)
      }
      context.system.eventStream.publish(TopHashTags(topHashTagsInTweets))
    }
  }

  def topHashTagsInTweets: String = formatCountMap(hashTagCounts)

}
