package core

import akka.actor.Actor
import domain.Tweet

/**
 * Created by Shawn on 11/9/2014.
 */
class TweetTimerActor extends Actor {
  var startTime: Option[Long] = None

  context.system.eventStream.subscribe(self, PrintReport.getClass)

  override def receive: Receive = {
    case tweet: Tweet => if(startTime.isEmpty) {
      startTime = Some(System.currentTimeMillis())
    }
    case PrintReport =>
      startTime.map {
        startupTime =>
          context.system.eventStream.publish(ElapsedTime(System.currentTimeMillis() - startupTime))
      }
  }


}
