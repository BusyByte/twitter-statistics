package core

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import domain.{HashTag, User, Tweet}
import org.scalatest.BeforeAndAfter


object TestTweet {
  type TestTweet = Tweet
  
  def apply(): TestTweet = {
    new Tweet(
      id = "TweetCounterActorTestId",
      user = User(id = "TweetCounterActorTestUser", "en", 20),
      place = None,
      text = "foo bar",
      urls = Nil,
      hashtags = Nil,
      photos = Nil
    )
  }
}

/**
 * Created by Shawn on 3/14/2015.
 */
class TweetCounterActorTest extends TestKit(ActorSystem()) with org.scalatest.FunSuiteLike with ImplicitSender with BeforeAndAfter {
  import ActorTestingSupport._

  before {
    system.eventStream.subscribe(self, classOf[TweetCount])
  }

  after {
    system.eventStream.unsubscribe(self, classOf[TweetCount])
  }

  test("Single tweet gets counted once") {
    val tweetCounter = TestActorRef(Props[TweetCounterActor])
    tweetCounter ! TestTweet()

    expectMsg(MSG_TIMEOUT, TweetCount(1))
  }

  test("Multiple tweets gets counted") {
    val tweetCounter = TestActorRef(Props[TweetCounterActor])
    tweetCounter ! TestTweet()
    tweetCounter ! TestTweet()
    tweetCounter ! TestTweet()

    expectMsg(MSG_TIMEOUT, TweetCount(1))
    expectMsg(MSG_TIMEOUT, TweetCount(2))
    expectMsg(MSG_TIMEOUT, TweetCount(3))
  }
}
