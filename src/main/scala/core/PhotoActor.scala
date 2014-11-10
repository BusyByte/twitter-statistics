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
    val containsPhotoLink = tweet.photos.exists {
      photo =>
        photo.displayUrl.toLowerCase.contains("pic.twitter.com") || photo.displayUrl.toLowerCase.contains("instagram")
    }

    if(containsPhotoLink) {
      numberOfPhotoTweets = numberOfPhotoTweets + 1
      context.system.eventStream.publish(PhotoCount(numberOfPhotoTweets))
    }
  }

}
