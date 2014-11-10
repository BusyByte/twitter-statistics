package core


import akka.actor.{Actor, Props}
import domain.Tweet

import scala.concurrent.duration._

/**
 * Created by Shawn on 11/8/2014.
 */
class TweetAnalysisActor extends Actor {

  val counter = context.system.actorOf(Props[TweetCounterActor], "counter")
  val emoji = context.system.actorOf(Props[EmojiActor], "emoji")
  val url = context.system.actorOf(Props[UrlActor], "url")
  val hashTag = context.system.actorOf(Props[HashTagActor], "hashtag")
  val photo = context.system.actorOf(Props[PhotoActor], "photo")
  val reporter = context.system.actorOf(Props[ReportActor], "reporter")

  context.system.scheduler.schedule(5 seconds, 10 seconds, self, PrintReport)(context.dispatcher)

  override def receive: Receive = {
    case tweet: Tweet => updateTweetStats(tweet)
    case PrintReport => reporter ! PrintReport
  }

  def updateTweetStats(tweet: Tweet): Unit = {
    counter ! tweet
    emoji ! tweet
    url ! tweet
    hashTag ! tweet
    photo ! tweet
  }


}
