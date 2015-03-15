package core

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.BeforeAndAfter


/**
 * Created by Shawn on 3/14/2015.
 */
class ReportActorTest extends TestKit(ActorSystem()) with org.scalatest.FunSuiteLike with ImplicitSender with BeforeAndAfter {

  import core.ActorTestingSupport._

  before {
    system.eventStream.subscribe(self, classOf[Report])
  }

  after {
    system.eventStream.unsubscribe(self, classOf[Report])
  }

  test("default report") {
    val reportActor = TestActorRef(Props[ReportActor])
    reportActor ! PrintReport

    val expectedReport = Report(
      totalTweets = s"total number of tweets received: 0",
      tweetsInTimeframe = s"average tweets per hour/minute/second: 0/0/0",
      topEmojis = s"top 10 emojis: ",
      tweetsWithEmoji = s"tweets w/ an emoji: 0%",
      topHashTags = s"top 10 hashtags: ",
      tweetsWithUrl = s"tweets w/ a url: 0%",
      tweetsWithPhotoUrl = s"tweets w/ photo url: 0%",
      topDomains = s"top 10 domains of urls in tweets: "
    )

    expectMsg(MSG_TIMEOUT, expectedReport)
  }


  test("basic report") {
    val reportActor = TestActorRef(Props[ReportActor])

    reportActor ! TweetCount(3)
    reportActor ! TopEmojis(summaryText = "Top Emoji Summary Text")
    reportActor ! EmojiCount(1)
    reportActor ! TopHashTags(summaryText = "Top Hash Tag Text")
    reportActor ! UrlCount(1)
    reportActor ! PhotoCount(1)
    reportActor ! TopDomains(summaryText = "Top Domain Text")

    reportActor ! PrintReport

    val expectedReport = Report(
      totalTweets = s"total number of tweets received: 3",
      tweetsInTimeframe = s"average tweets per hour/minute/second: 0/0/0",
      topEmojis = s"top 10 emojis: Top Emoji Summary Text",
      tweetsWithEmoji = s"tweets w/ an emoji: 33%",
      topHashTags = s"top 10 hashtags: Top Hash Tag Text",
      tweetsWithUrl = s"tweets w/ a url: 33%",
      tweetsWithPhotoUrl = s"tweets w/ photo url: 33%",
      topDomains = s"top 10 domains of urls in tweets: Top Domain Text"
    )

    expectMsg(MSG_TIMEOUT, expectedReport)
  }


  test("advanced report") {
    val reportActor = TestActorRef(Props[ReportActor])
    Thread.sleep(2 * 1000)
    reportActor ! TweetCount(1000)
    reportActor ! TopEmojis(summaryText = "Top Emoji Summary Text")
    reportActor ! EmojiCount(10)
    reportActor ! TopHashTags(summaryText = "Top Hash Tag Text")
    reportActor ! UrlCount(10)
    reportActor ! PhotoCount(10)
    reportActor ! TopDomains(summaryText = "Top Domain Text")

    reportActor ! PrintReport

    val expectedReport = Report(
      totalTweets = s"total number of tweets received: 1000",
      tweetsInTimeframe = s"average tweets per hour/minute/second: 1800000/30000/500",
      topEmojis = s"top 10 emojis: Top Emoji Summary Text",
      tweetsWithEmoji = s"tweets w/ an emoji: 1%",
      topHashTags = s"top 10 hashtags: Top Hash Tag Text",
      tweetsWithUrl = s"tweets w/ a url: 1%",
      tweetsWithPhotoUrl = s"tweets w/ photo url: 1%",
      topDomains = s"top 10 domains of urls in tweets: Top Domain Text"
    )

    expectMsg(MSG_TIMEOUT, expectedReport)
  }

}
