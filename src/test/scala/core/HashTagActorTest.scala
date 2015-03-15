package core

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import domain.{HashTag, User, Tweet}
import org.scalatest.BeforeAndAfter


object HashTagTweet {
  type HashTagTweet = Tweet
  
  def apply(hashTagTextValues: List[String]): HashTagTweet = {
    new Tweet(
      id = "HashTagActorTestId",
      user = User(id = "HashTagActorTestUser", "en", 20),
      place = None,
      text = "foo bar",
      urls = Nil,
      hashtags = hashTagTextValues.map(HashTag),
      photos = Nil
    )
  }
}

/**
 * Created by Shawn on 3/14/2015.
 */
class HashTagActorTest extends TestKit(ActorSystem()) with org.scalatest.FunSuiteLike with ImplicitSender with BeforeAndAfter {
  import ActorTestingSupport._

  before {
    system.eventStream.subscribe(self, classOf[TopHashTags])
  }

  after {
    system.eventStream.unsubscribe(self, classOf[TopHashTags])
  }

  test("Single Hashtag gets counted once") {
    val hashTagActor = TestActorRef(Props[HashTagActor])
    hashTagActor ! HashTagTweet(List("voteforfoo"))

    expectMsg(MSG_TIMEOUT, TopHashTags("[{\"voteforfoo\" : \"1\"}]"))
  }

  test("Single Hashtag with double occurrence gets counted twice") {
    val hashTagActor = TestActorRef(Props[HashTagActor])
    hashTagActor ! HashTagTweet(List("voteforfoo", "voteforfoo"))

    expectMsg(MSG_TIMEOUT, TopHashTags("[{\"voteforfoo\" : \"2\"}]"))
  }

  test("Single Hashtag two tweets gets counted twice") {
    val hashTagActor = TestActorRef(Props[HashTagActor])
    hashTagActor ! HashTagTweet(List("voteforfoo"))
    hashTagActor ! HashTagTweet(List("voteforfoo"))

    expectMsg(MSG_TIMEOUT, TopHashTags("[{\"voteforfoo\" : \"1\"}]"))
    expectMsg(MSG_TIMEOUT, TopHashTags("[{\"voteforfoo\" : \"2\"}]"))
  }

  test("No Hash") {
    val hashTagActor = TestActorRef(Props[HashTagActor])
    hashTagActor ! HashTagTweet(Nil)

    expectNoMsg(MSG_TIMEOUT)
  }

}
