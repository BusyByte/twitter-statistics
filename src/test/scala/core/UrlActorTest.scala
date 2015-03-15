package core

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import domain.{Url, HashTag, User, Tweet}
import org.scalatest.BeforeAndAfter


object UrlTweet {
  type UrlTweet = Tweet
  
  def apply(urlTextValues: List[String]): UrlTweet = {
    new Tweet(
      id = "UrlActorTestId",
      user = User(id = "UrlActorTestUser", "en", 20),
      place = None,
      text = "foo bar",
      urls = urlTextValues.map(expandedUrlText => Url(expandedUrl = expandedUrlText, displayUrl = "displayUrl")),
      hashtags = Nil,
      photos = Nil
    )
  }
}

/**
 * Created by Shawn on 3/14/2015.
 */
class UrlActorTest extends TestKit(ActorSystem()) with org.scalatest.FunSuiteLike with ImplicitSender with BeforeAndAfter {
  import ActorTestingSupport._

  before {
    system.eventStream.subscribe(self, classOf[UrlCount])
    system.eventStream.subscribe(self, classOf[TopDomains])
  }

  after {
    system.eventStream.unsubscribe(self, classOf[UrlCount])
    system.eventStream.unsubscribe(self, classOf[TopDomains])
  }

  test("Single Url gets counted once") {
    val urlActor = TestActorRef(Props[UrlActor])
    urlActor ! UrlTweet(List("http://www.domain1.com/some/url"))

    expectMsg(MSG_TIMEOUT, UrlCount(1))
    expectMsg(MSG_TIMEOUT, TopDomains("[{\"www.domain1.com\" : \"1\"}]"))
  }

  test("Multiple Url with same host tweet gets counted once, counted multiple for host") {
    val urlActor = TestActorRef(Props[UrlActor])
    urlActor ! UrlTweet(List("http://www.domain1.com/some/url", "https://www.domain1.com/some/url2"))

    expectMsg(MSG_TIMEOUT, UrlCount(1))
    expectMsg(MSG_TIMEOUT, TopDomains("[{\"www.domain1.com\" : \"2\"}]"))
  }

  test("Multiple tweets with same Url") {
    val urlActor = TestActorRef(Props[UrlActor])
    urlActor ! UrlTweet(List("http://www.domain1.com/some/url"))
    urlActor ! UrlTweet(List("https://www.domain1.com/some/url2"))

    expectMsg(MSG_TIMEOUT, UrlCount(1))
    expectMsg(MSG_TIMEOUT, TopDomains("[{\"www.domain1.com\" : \"1\"}]"))

    expectMsg(MSG_TIMEOUT, UrlCount(2))
    expectMsg(MSG_TIMEOUT, TopDomains("[{\"www.domain1.com\" : \"2\"}]"))
  }

  test("Multiple tweets with different Url") {
    val urlActor = TestActorRef(Props[UrlActor])
    urlActor ! UrlTweet(List("http://www.domain1.com/some/url"))
    urlActor ! UrlTweet(List("https://www.domain2.com/some/url2"))

    expectMsg(MSG_TIMEOUT, UrlCount(1))
    expectMsg(MSG_TIMEOUT, TopDomains("[{\"www.domain1.com\" : \"1\"}]"))

    expectMsg(MSG_TIMEOUT, UrlCount(2))
    expectMsg(MSG_TIMEOUT, TopDomains("[{\"www.domain1.com\" : \"1\"},{\"www.domain2.com\" : \"1\"}]"))
  }

  test("Tweets with no is not counted") {
    val urlActor = TestActorRef(Props[UrlActor])
    urlActor ! UrlTweet(List())

    expectNoMsg(MSG_TIMEOUT)
  }

}
