package net.nomadicalien.twitter.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import io.circe._
import io.circe.generic.semiauto._
import net.nomadicalien.twitter.models._
import cats.implicits._

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object TwitterStream {
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer(
    materializerSettings =
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(Supervision.resumingDecider: Supervision.Decider)
        .withDebugLogging(true)
  )
  implicit val httpExt = Http()
  val consumer = new DefaultConsumerService(actorSystem.dispatcher)

  def getEnvVar(envVarName: String) = scala.sys.env.get(envVarName)
    .fold[Either[ApplicationError, String]](Left(MissingConfigError(s"$envVarName environment variable is missing")))(Right.apply)

  def maybeConsumerKey = getEnvVar("TWITTER_CONSUMER_KEY")

  def maybeConsumerSecret = getEnvVar("TWITTER_CONSUMER_SECRET")

  def maybeAccessToken = getEnvVar("TWITTER_ACCESS_TOKEN")

  def maybeAccessTokenSecret = getEnvVar("TWITTER_ACCESS_TOKEN_SECRET")

  val statsSampleUrl = "https://stream.twitter.com/1.1/statuses/sample.json?stall_warnings=true"

  def twitterStream: Either[ApplicationError, Future[Either[ApplicationError, Source[Either[ApplicationError, TwitterStatusApiModel], Any]]]] = {
    for {
      consumerKey <- maybeConsumerKey
      consumerSecret <- maybeConsumerSecret
      accessToken <- maybeAccessToken
      accessTokenSecret <- maybeAccessTokenSecret
      _ = println("getting auth header")
      auth = getAuthorizationHeader(consumerKey, consumerSecret, accessToken, accessTokenSecret)
      _ = println("got auth header")
      headers = auth.map(createHeaders)
      request = headers.map(createRequest)
      _ = println("executing request")
      response = request.flatMap(httpExt.singleRequest(_))
    } yield response.map(handleResponse)
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

  def toTweatStream(dataBytes: Source[ByteString, Any]): Source[Either[ApplicationError, TwitterStatusApiModel], Any] = {
    dataBytes.scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
      .filter(_.contains("\r\n"))
      .map(decodeJson)
  }

  def handleResponse(response: HttpResponse): Either[ApplicationError, Source[Either[ApplicationError, TwitterStatusApiModel], Any]] = {
    println("handling response")
    response.status match {
      case StatusCodes.OK =>
        Right(toTweatStream(response.entity.dataBytes))

      case status =>
        Left(HttpError(s"Received a bad status code: $status"))
    }
  }

  def getAuthorizationHeader(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String): Future[String] =
    consumer.createOauthenticatedRequest(
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
