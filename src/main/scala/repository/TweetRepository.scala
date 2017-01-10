package net.nomadicalien.twitter.repository

import akka.NotUsed
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import cats.implicits._
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.{ConsumerService, DefaultConsumerService}
import net.nomadicalien.twitter.actor.ActorModule
import net.nomadicalien.twitter.{Config, EnvVariableConfig}
import net.nomadicalien.twitter.models._
import net.nomadicalien.twitter.stream.StreamUtils

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait TweetRepository {
  def tweetSampleStream: Source[Either[ApplicationError, TweetStatus], NotUsed]
}

private[repository] trait TweetRepositoryInterpreter extends TweetRepository with StreamUtils {
  def consumerService: ConsumerService
  def config: Config
  def httpExt: HttpExt

  val statsSampleUrl = "https://stream.twitter.com/1.1/statuses/sample.json?stall_warnings=true"

  def requestFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    httpExt.outgoingConnectionHttps("stream.twitter.com")


  def tweetSampleStream: Source[Either[ApplicationError, TweetStatus], NotUsed] = {
    val maybeStream: Either[ApplicationError, Source[Either[ApplicationError, TweetStatus], NotUsed]] = for {
      consumerKey <- config.maybeConsumerKey
      consumerSecret <- config.maybeConsumerSecret
      accessToken <- config.maybeAccessToken
      accessTokenSecret <- config.maybeAccessTokenSecret
      auth = getAuthorizationHeader(consumerKey, consumerSecret, accessToken, accessTokenSecret)
      headers = createHeaders(auth)
      request = createRequest(headers)
      responseSource: Source[HttpResponse, NotUsed] = Source.single(request).via(requestFlow)
    } yield responseSource.flatMapConcat(handleResponse)

    maybeStream match {
      case Right(s) => s
      case Left(e) => Source.single[Either[ApplicationError, TweetStatus]](Left(e))
    }
  }

  def stringFlow = Flow[String].map(Tweet.decodeTwitterStatusApiModel)

  def convertBytesToTweetStream(dataBytes: Source[ByteString, Any]): Source[Either[ApplicationError, TweetStatus], Any] = {
    dataBytes.scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
      .filter(_.contains("\r\n")).async
      .via(balancer(stringFlow, 8))
  }

  def handleResponse(response: HttpResponse): Source[Either[ApplicationError, TweetStatus], Any] = {
    response.status match {
      case StatusCodes.OK =>
        convertBytesToTweetStream(response.entity.withoutSizeLimit().dataBytes)

      case status =>
        Source.single[Either[ApplicationError, TweetStatus]](Left(HttpError(s"Received a bad status code: $status")))
    }
  }

  def getAuthorizationHeader(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String): String = {
    val authHeaderF = consumerService.createOauthenticatedRequest(
      KoauthRequest(
        method = "GET",
        url = statsSampleUrl,
        authorizationHeader = None,
        body = None
      ),
      consumerKey,
      consumerSecret,
      accessToken,
      accessTokenSecret
    ).map(_.header)

    Await.result(authHeaderF, 10 minutes)
  }

  def createHeaders(authHeaderValue: String): immutable.Seq[HttpHeader] = {
    immutable.Seq(
      RawHeader("Authorization", authHeaderValue),
      Accept(immutable.Seq(MediaRanges.`*/*`))
    )
  }

  def createRequest(headers: immutable.Seq[HttpHeader]): HttpRequest = {
    HttpRequest(
      HttpMethods.GET,
      uri = Uri(statsSampleUrl),
      headers = headers
    )
  }

}


object TweetRepository extends TweetRepositoryInterpreter {
  lazy val consumerService: ConsumerService = new DefaultConsumerService(scala.concurrent.ExecutionContext.global)
  lazy val config: Config = EnvVariableConfig
  lazy val httpExt: HttpExt = ActorModule.httpExt
}



