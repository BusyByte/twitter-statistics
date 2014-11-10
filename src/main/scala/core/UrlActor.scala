package core

import java.net.URL

import akka.actor.Actor
import domain.Tweet

import scala.collection.mutable

/**
 * Created by Shawn on 11/9/2014.
 */
class UrlActor extends Actor {
  var numberOfUrlTweets: Int = 0
  val domainCounts = mutable.Map[String, Int]().withDefaultValue(0)

  override def receive: Receive = {
    case tweet: Tweet => updateUrlCounts(tweet)
  }

  def updateUrlCounts(tweet: Tweet): Unit = {
    if(tweet.urls.nonEmpty) {
      incrementUrlCounts()
      tweet.urls.foreach {
        tweetUrl =>
          val theUrl = new URL(tweetUrl.expandedUrl)
          val domain = theUrl.getHost.toLowerCase
          val count = domainCounts(domain)
          domainCounts.update(domain, count + 1)
      }

      context.system.eventStream.publish(UrlCount(numberOfUrlTweets))
      context.system.eventStream.publish(TopDomains(topDomainsInTweets))
    }
  }

  def incrementUrlCounts(): Unit = {
    numberOfUrlTweets = numberOfUrlTweets + 1
  }

  def topDomainsInTweets: String = formatCountMap(domainCounts)
}
