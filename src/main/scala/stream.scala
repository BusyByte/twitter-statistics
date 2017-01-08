package net.nomadicalien.twitter.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, FlowShape, Supervision}
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

  def shutdown(): Unit = {
    actorSystem.terminate()
  }
  val consumer = new DefaultConsumerService(scala.concurrent.ExecutionContext.global)

  def getEnvVar(envVarName: String) = scala.sys.env.get(envVarName)
    .fold[Either[ApplicationError, String]](Left(MissingConfigError(s"$envVarName environment variable is missing")))(Right.apply)

  def maybeConsumerKey = getEnvVar("TWITTER_CONSUMER_KEY")

  def maybeConsumerSecret = getEnvVar("TWITTER_CONSUMER_SECRET")

  def maybeAccessToken = getEnvVar("TWITTER_ACCESS_TOKEN")

  def maybeAccessTokenSecret = getEnvVar("TWITTER_ACCESS_TOKEN_SECRET")

  val statsSampleUrl = "https://stream.twitter.com/1.1/statuses/sample.json?stall_warnings=true"

  lazy val requestFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    httpExt.outgoingConnectionHttps("stream.twitter.com")

  lazy val twitterStream: Source[Either[ApplicationError, TwitterStatusApiModel], NotUsed] = {
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
  implicit val warningDecoder: Decoder[Warning] = deriveDecoder[Warning]
  implicit val streamWarningDecoder: Decoder[StreamWarning] = deriveDecoder[StreamWarning]
  implicit val mediaDecoder: Decoder[Media] = deriveDecoder[Media]
  implicit val urlDecoder: Decoder[Url] = deriveDecoder[Url]
  implicit val hashTagDecoder: Decoder[HashTag] = deriveDecoder[HashTag]
  implicit val entititesDecoder: Decoder[Entities] = deriveDecoder[Entities]
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]
  implicit val statusDecoder: Decoder[Status] = deriveDecoder[Status]
  implicit val deleteDecoder: Decoder[Delete] = deriveDecoder[Delete]
  implicit val deletedTweetDecoder: Decoder[DeletedTweet] = deriveDecoder[DeletedTweet]
  import io.circe._, io.circe.parser._
  def decodeJson(json: String): Either[ApplicationError, TwitterStatusApiModel] = {
      decode[DeletedTweet](json).orElse(decode[StreamWarning](json)).orElse(decode[Tweet](json))
      .leftMap{e =>
        val prettyJson = parse(json).toOption.map(_.toString()).getOrElse("")
        TweetParseError(s"${e.getMessage}:Error parsing json:\n${prettyJson}\n" )
      }
      .toEither
  }

  val stringFlow = Flow[String].map(decodeJson).async

  def convertBytesToTweetStream(dataBytes: Source[ByteString, Any]): Source[Either[ApplicationError, TwitterStatusApiModel], Any] = {
    dataBytes.scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
      .filter(_.contains("\r\n")).async
      .via(balancer(stringFlow, 3))
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

  def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Flow[In, Out, NotUsed] = {
    import akka.stream.scaladsl.GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](workerCount))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }

}
