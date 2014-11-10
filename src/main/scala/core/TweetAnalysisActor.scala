package core


import akka.actor.{ActorRef, Actor, Props}
import akka.routing.BroadcastRouter
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

  var broadcastRouter = context.system.actorOf(Props.empty.withRouter(BroadcastRouter(routees = List[ActorRef](counter, emoji, url, hashTag, photo))))

  context.system.scheduler.schedule(5 seconds, 10 seconds, self, PrintReport)(context.dispatcher)

  override def receive: Receive = {
    case tweet: Tweet => broadcastRouter ! tweet
    case PrintReport => reporter ! PrintReport
  }
}
