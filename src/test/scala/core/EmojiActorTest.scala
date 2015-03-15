package core

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import domain.{User, Tweet}
import org.scalatest.BeforeAndAfter
import scala.concurrent.duration._

object EmojiTweet {
  type EmojiTweet = Tweet
  
  def apply(tweetText: String): EmojiTweet = {
    new Tweet(
      id = "EmojiActorTestId",
      user = User(id = "EmojiActorTestUser", "en", 20),
      place = None,
      text = tweetText,
      urls = Nil,
      hashtags = Nil,
      photos = Nil
    )
  }
}

/**
 * Created by Shawn on 3/14/2015.
 */
class EmojiActorTest extends TestKit(ActorSystem()) with org.scalatest.FunSuiteLike with ImplicitSender with BeforeAndAfter {
  import ActorTestingSupport._

  before {
    system.eventStream.subscribe(self, classOf[TopEmojis])
    system.eventStream.subscribe(self, classOf[EmojiCount])
  }

  after {
    system.eventStream.unsubscribe(self, classOf[TopEmojis])
    system.eventStream.unsubscribe(self, classOf[EmojiCount])
  }

  test("Single Emoji gets counted once") {
    val emojiActor = TestActorRef(Props[EmojiActor])
    emojiActor ! EmojiTweet(":p")

    expectMsg(MSG_TIMEOUT, TopEmojis("[{\"FACE WITH STUCK-OUT TONGUE\" : \"1\"}]"))
    expectMsg(MSG_TIMEOUT, EmojiCount(1))
  }

  test("Single emoji gets counted once") {
    val emojiActor = TestActorRef(Props[EmojiActor], "emojiActor")

    emojiActor ! EmojiTweet(":p")

    expectMsg(MSG_TIMEOUT, TopEmojis("[{\"FACE WITH STUCK-OUT TONGUE\" : \"1\"}]"))
    expectMsg(MSG_TIMEOUT, EmojiCount(1))
  }

  test("Emoji double occurrence gets counted as one tweet, emoji itself is counted twice") {
    val emojiActor = TestActorRef(Props[EmojiActor])

    emojiActor ! EmojiTweet(":p :p")

    expectMsg(MSG_TIMEOUT, TopEmojis("[{\"FACE WITH STUCK-OUT TONGUE\" : \"2\"}]"))
    expectMsg(MSG_TIMEOUT, EmojiCount(1))
  }

  test("Multiple Emoji counted") {
    val emojiActor = TestActorRef(Props[EmojiActor])

    emojiActor ! EmojiTweet(":p :)")

    expectMsg(MSG_TIMEOUT, TopEmojis("[{\"FACE WITH STUCK-OUT TONGUE\" : \"1\"},{\"SMILING FACE WITH OPEN MOUTH AND SMILING EYES\" : \"1\"},{\"SMILING FACE WITH OPEN MOUTH\" : \"1\"}]"))
    expectMsg(MSG_TIMEOUT, EmojiCount(1))
  }

  test("No emoji doesn't get counted") {
    val emojiActor = TestActorRef(Props[EmojiActor])

    emojiActor ! EmojiTweet("foo bar")

    expectNoMsg(MSG_TIMEOUT)
  }

  test("Single Emoji with multiple tweets gets counted twice") {
    val emojiActor = TestActorRef(Props[EmojiActor])
    emojiActor ! EmojiTweet(":p")
    emojiActor ! EmojiTweet(":p")

    expectMsg(MSG_TIMEOUT, TopEmojis("[{\"FACE WITH STUCK-OUT TONGUE\" : \"1\"}]"))
    expectMsg(MSG_TIMEOUT, EmojiCount(1))

    expectMsg(MSG_TIMEOUT, TopEmojis("[{\"FACE WITH STUCK-OUT TONGUE\" : \"2\"}]"))
    expectMsg(MSG_TIMEOUT, EmojiCount(2))
  }

}
