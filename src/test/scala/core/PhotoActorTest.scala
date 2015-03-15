package core

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import domain._
import org.scalatest.BeforeAndAfter


object PhotoTweet {
  type PhotoTweet = Tweet
  
  def apply(photoUrls: List[String] = Nil, urlList: List[String] = Nil): PhotoTweet = {
    new Tweet(
      id = "PhotoActorTestId",
      user = User(id = "PhotoActorTestUser", "en", 20),
      place = None,
      text = "foo bar",
      urls = urlList.map(urlText => Url(expandedUrl = "foo", displayUrl = urlText)),
      hashtags = Nil,
      photos = photoUrls.map(Photo)
    )
  }
}

/**
 * Created by Shawn on 3/14/2015.
 */
class PhotoActorTest extends TestKit(ActorSystem()) with org.scalatest.FunSuiteLike with ImplicitSender with BeforeAndAfter {
  import ActorTestingSupport._

  before {
    system.eventStream.subscribe(self, classOf[PhotoCount])
  }

  after {
    system.eventStream.unsubscribe(self, classOf[PhotoCount])
  }

  test("Single Twitter Photo gets tweet counted once") {
    val photoActor = TestActorRef(Props[PhotoActor])
    photoActor ! PhotoTweet(photoUrls = List("pic.twitter.com"))

    expectMsg(MSG_TIMEOUT, PhotoCount(1))
  }

  test("Single Instagram Photo gets tweet counted once") {
    val photoActor = TestActorRef(Props[PhotoActor])
    photoActor ! PhotoTweet(photoUrls = List("instagram"))

    expectMsg(MSG_TIMEOUT, PhotoCount(1))
  }

  test("Single Twitter Url gets tweet counted once") {
    val photoActor = TestActorRef(Props[PhotoActor])
    photoActor ! PhotoTweet(urlList = List("pic.twitter.com"))

    expectMsg(MSG_TIMEOUT, PhotoCount(1))
  }

  test("Single Instagram Url gets tweet counted once") {
    val photoActor = TestActorRef(Props[PhotoActor])
    photoActor ! PhotoTweet(urlList = List("instagram"))

    expectMsg(MSG_TIMEOUT, PhotoCount(1))
  }

  test("Multiple Photo and Url gets tweet counted once") {
    val multiUrls = List("pic.twitter.com", "instagram","pic.twitter.com", "instagram")
    val photoActor = TestActorRef(Props[PhotoActor])
    photoActor ! PhotoTweet(photoUrls = multiUrls, urlList = multiUrls)

    expectMsg(MSG_TIMEOUT, PhotoCount(1))
  }

  test("Multiple Tweets with Url gets counted") {
    val photoActor = TestActorRef(Props[PhotoActor])
    photoActor ! PhotoTweet(photoUrls = List("pic.twitter.com"))
    photoActor ! PhotoTweet(photoUrls = List("instagram"))
    photoActor ! PhotoTweet(urlList = List("pic.twitter.com"))
    photoActor ! PhotoTweet(urlList = List("instagram"))

    expectMsg(MSG_TIMEOUT, PhotoCount(1))
    expectMsg(MSG_TIMEOUT, PhotoCount(2))
    expectMsg(MSG_TIMEOUT, PhotoCount(3))
    expectMsg(MSG_TIMEOUT, PhotoCount(4))
  }
}
