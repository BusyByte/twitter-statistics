package net.nomadicalien.twitter.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.util.ByteString
import cats.implicits._
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import io.circe._
import io.circe.generic.semiauto._
import net.nomadicalien.twitter.models._

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object TwitterStream {
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer(
    materializerSettings =
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(Supervision.resumingDecider: Supervision.Decider)
  )
  implicit val httpExt = Http()
  val consumer = new DefaultConsumerService(scala.concurrent.ExecutionContext.global)

  def getEnvVar(envVarName: String) = scala.sys.env.get(envVarName)
    .fold[Either[ApplicationError, String]](Left(MissingConfigError(s"$envVarName environment variable is missing")))(Right.apply)

  def maybeConsumerKey = getEnvVar("TWITTER_CONSUMER_KEY")

  def maybeConsumerSecret = getEnvVar("TWITTER_CONSUMER_SECRET")

  def maybeAccessToken = getEnvVar("TWITTER_ACCESS_TOKEN")

  def maybeAccessTokenSecret = getEnvVar("TWITTER_ACCESS_TOKEN_SECRET")

  val statsSampleUrl = "https://stream.twitter.com/1.1/statuses/sample.json?stall_warnings=true"

  val requestFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    httpExt.outgoingConnectionHttps("stream.twitter.com")

  def twitterStream: Source[Either[ApplicationError, TwitterStatusApiModel], NotUsed] = {
    val maybeStream: Either[ApplicationError, Source[Either[ApplicationError, TwitterStatusApiModel], NotUsed]] = for {
      consumerKey <- maybeConsumerKey
      consumerSecret <- maybeConsumerSecret
      accessToken <- maybeAccessToken
      accessTokenSecret <- maybeAccessTokenSecret
      auth = getAuthorizationHeader(consumerKey, consumerSecret, accessToken, accessTokenSecret)
      headers = createHeaders(auth)
      request = createRequest(headers)
      responseSource: Source[HttpResponse, NotUsed] = Source.single(request).via(requestFlow)
    } yield responseSource.flatMapConcat(handleResponse)

    maybeStream match {
      case Right(s) => s
      case Left(e) => Source.single[Either[ApplicationError, TwitterStatusApiModel]](Left(e))
    }
  }

  import io.circe.parser.decode
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]
  implicit val statusDecoder: Decoder[Status] = deriveDecoder[Status]
  implicit val deleteDecoder: Decoder[Delete] = deriveDecoder[Delete]
  implicit val deletedTweetDecoder: Decoder[DeletedTweet] = deriveDecoder[DeletedTweet]

  def decodeJson(json: String): Either[ApplicationError, TwitterStatusApiModel] = {
    decode[Tweet](json)
      .orElse(decode[DeletedTweet](json))
      .leftMap(_ => TweetParseError(s"Error parsing json:\n$json\n" ))
      .toEither
  }

  def convertBytesToTweetStream(dataBytes: Source[ByteString, Any]): Source[Either[ApplicationError, TwitterStatusApiModel], Any] = {
    dataBytes.scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
      .filter(_.contains("\r\n"))
      .map(decodeJson)
  }

  def handleResponse(response: HttpResponse): Source[Either[ApplicationError, TwitterStatusApiModel], Any] = {
    response.status match {
      case StatusCodes.OK =>
        convertBytesToTweetStream(response.entity.withoutSizeLimit().dataBytes)

      case status =>
        Source.single[Either[ApplicationError, TwitterStatusApiModel]](Left(HttpError(s"Received a bad status code: $status")))
    }
  }

  import scala.concurrent.duration.DurationInt

  def getAuthorizationHeader(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String): String = {
    val authHeaderF = consumer.createOauthenticatedRequest(
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
