package core

import akka.actor.{Props, ActorSystem}
import scala.annotation.tailrec

object Main extends App {
  import Commands._

  val system = ActorSystem()
  val sentiment = system.actorOf(Props(new SentimentAnalysisActor with CSVLoadedSentimentSets with AnsiConsoleSentimentOutput))
  val stream = system.actorOf(Props(new TweetStreamerActor(TweetStreamerActor.twitterUri, sentiment) with OAuthTwitterAuthorization))

  @tailrec
  private def commandLoop(): Unit = {
    Console.readLine() match {
      case QuitCommand         => return
      case BeginCommand => stream ! BeginStreaming
      case _                   => println("WTF??!!")
    }

    commandLoop()
  }

  // start processing the commands
  commandLoop()

}

object Commands {

  val QuitCommand   = "quit"
  val BeginCommand = "begin"

}
