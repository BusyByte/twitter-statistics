package core

import akka.actor.Actor
import domain.Tweet

/**
 * Created by Shawn on 11/9/2014.
 */
class TweetCounterActor extends Actor {
  var numberOfTweets: Int = 0
  override def receive: Receive = {
    case tweet: Tweet =>
      numberOfTweets = numberOfTweets + 1
      context.system.eventStream.publish(TweetCount(numberOfTweets))
  }
}
