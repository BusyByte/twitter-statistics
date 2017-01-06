package net.nomadicalien.twitter.stream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.Source
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import io.circe._
import io.circe.generic.semiauto._
import net.nomadicalien.twitter.models.{ApplicationError, MissingConfigError, Tweet}
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

  val statsSampleUrl = "https://stream.twitter.com/1.1/statuses/sample.json"

  def twitterStream: Either[ApplicationError, Future[Source[Tweet, Any]]] = {
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


  implicit val fooDecoder: Decoder[Tweet] = deriveDecoder[Tweet]

  def handleResponse(response: HttpResponse): Source[Tweet, Any] = {
    println("handling response")
    response.status match {
      case StatusCodes.OK => response.entity.dataBytes.via(de.knutwalker.akka.stream.support.CirceStreamSupport.decode[Tweet])
      case status =>
        response.entity.dataBytes.runFold("")(_ + _).onComplete {
          case Success(s) => println(s"Received non-normal status code: $status, response $s")
          case Failure(f) => println(s"Received non-normal status code: $status, response ${f.getMessage}")
        }

        Source.empty[Tweet]
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
