package core

import akka.actor.Actor
import domain.Tweet

/**
 * Created by Shawn on 11/9/2014.
 */
class PhotoActor extends Actor {

  var numberOfPhotoTweets: Int = 0

  override def receive: Receive = {
    case tweet: Tweet =>  updatePhotoCounts(tweet)
  }

  def updatePhotoCounts(tweet: Tweet): Unit = {
    val allUrls = tweet.photos.map(_.displayUrl) ++ tweet.urls.map(_.displayUrl)
    val anyWithLink = allUrls.exists(containsPhotoLink)

    if(anyWithLink) {
      numberOfPhotoTweets = numberOfPhotoTweets + 1
      context.system.eventStream.publish(PhotoCount(numberOfPhotoTweets))
    }
  }

  private def containsPhotoLink(url: String): Boolean = {
    val lowerCaseUrl: String = url.toLowerCase
    lowerCaseUrl.contains("pic.twitter.com") || lowerCaseUrl.contains("instagram")
  }

}
