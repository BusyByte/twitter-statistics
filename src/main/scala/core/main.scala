package core

import akka.actor.{Props, ActorSystem}
import scala.annotation.tailrec

object Main extends App {

  val system = ActorSystem()
  val tweetAnalyzer = system.actorOf(Props[TweetAnalysisActor])
  val stream = system.actorOf(Props(new TweetStreamerActor(TweetStreamerActor.twitterUri, tweetAnalyzer) with OAuthTwitterAuthorization))

  stream ! BeginStreaming
}
