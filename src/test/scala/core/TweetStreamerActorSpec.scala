package core

import akka.actor.ActorSystem
import org.specs2.mutable.SpecificationLike
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import domain.{Url, HashTag, Tweet}
import spray.http.Uri

class TweetStreamerActorSpec extends TestKit(ActorSystem()) with SpecificationLike with ImplicitSender {
  sequential

  val port = 12345
  val tweetStream = TestActorRef(new TweetStreamerActor(Uri(s"http://localhost:$port/"), testActor) with TwitterAuthorization {
    def authorize = identity
  })

  "Streaming tweets" >> {

    "Should unmarshal one tweet" in {
      val twitterApi = TwitterApi(port)
      tweetStream ! BeginStreaming

      val tweet = expectMsgType[Tweet]
      tweet.text mustEqual "Aggressive Ponytail #freebandnames"
      tweet.user.lang mustEqual "en"
      tweet.user.id mustEqual "137238150"
      tweet.place mustEqual None
      tweet.hashtags mustEqual List(HashTag("freebandnames"))
      tweet.urls mustEqual List(Url("https://blog.twitter.com/2013/rich-photo-experience-now-in-embedded-tweets-3"))
      twitterApi.stop()
      success
    }
  }

}
