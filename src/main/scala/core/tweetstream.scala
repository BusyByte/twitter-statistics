package core

import spray.httpx.unmarshalling.{MalformedContent, Unmarshaller, Deserialized}
import spray.http._
import spray.json._
import spray.client.pipelining._
import akka.actor.{ActorRef, Actor}
import spray.http.HttpRequest
import domain._
import scala.io.Source
import scala.util.Try
import spray.can.Http
import akka.io.IO
import scala.concurrent.duration._

trait TwitterAuthorization {
  def authorize: HttpRequest => HttpRequest
}

trait OAuthTwitterAuthorization extends TwitterAuthorization {
  import OAuth._
  val home = System.getProperty("user.home")
  val lines = Source.fromFile(s"$home/.twitter/activator").getLines().toList

  val consumer = Consumer(lines(0), lines(1))
  val token = Token(lines(2), lines(3))

  val authorize: (HttpRequest) => HttpRequest = oAuthAuthorizer(consumer, token)
}

trait TweetMarshaller {

  implicit object TweetUnmarshaller extends Unmarshaller[Tweet] {

    def mkUser(user: JsObject): Deserialized[User] = {
      (user.fields("id_str"), user.fields("lang"), user.fields("followers_count")) match {
        case (JsString(id), JsString(lang), JsNumber(followers)) => Right(User(id, lang, followers.toInt))
        case (JsString(id), _, _)                                => Right(User(id, "", 0))
        case _                                                   => Left(MalformedContent("bad user"))
      }
    }

    def mkPlace(place: JsValue): Deserialized[Option[Place]] = place match {
      case JsObject(fields) =>
        (fields.get("country"), fields.get("name")) match {
          case (Some(JsString(country)), Some(JsString(name))) => Right(Some(Place(country, name)))
          case _                                               => Left(MalformedContent("bad place"))
        }
      case JsNull => Right(None)
      case _ => Left(MalformedContent("bad tweet"))
    }

    def mkHashTags(entities: Option[JsValue]): List[HashTag] = entities match {
      case Some(entitiesObj: JsObject) => entitiesObj.fields.get("hashtags") match {
        case Some(theArray: JsArray) if theArray.elements.nonEmpty => theArray.elements.map(value => mkHashTag(value.asJsObject))
        case _ => Nil
      }
      case _ => Nil
    }

    def mkHashTag(hashTag : JsObject): HashTag =  hashTag.fields.get("text") match {
      case Some(JsString(text)) => HashTag(text)
    }

    def mkUrls(entities: Option[JsValue]): List[Url] = entities match {
      case Some(entitiesObj: JsObject) => entitiesObj.fields.get("urls") match {
        case Some(theArray: JsArray) if theArray.elements.nonEmpty => theArray.elements.map(value => mkUrl(value.asJsObject))
        case _ => Nil
      }
      case _ => Nil
    }

    def mkUrl(url: JsObject): Url =  (url.fields.get("expanded_url"), url.fields.get("display_url")) match {
      case (Some(JsString(expanded_url)), Some(JsString(display_url))) => Url(expanded_url, display_url)
    }

    def mkPhotos(entities: Option[JsValue]): List[Photo] = entities match {
      case Some(entitiesObj: JsObject) => entitiesObj.fields.get("media") match {
        case Some(theArray: JsArray) if theArray.elements.nonEmpty => theArray.elements.map(value => mkPhoto(value.asJsObject))
        case _ => Nil
      }
      case _ => Nil
    }

    def mkPhoto(photo: JsObject): Photo =  photo.fields.get("display_url") match {
      case Some(JsString(display_url)) => Photo(display_url)
    }
    

    def apply(entity: HttpEntity): Deserialized[Tweet] = {
      Try {
        val entityString: String = entity.asString
        val json = JsonParser(entityString).asJsObject

        (json.fields.get("id_str"), json.fields.get("text"), json.fields.get("place"), json.fields.get("user")) match {
          case (Some(JsString(id)), Some(JsString(text)), Some(place), Some(user: JsObject)) =>
            val x = mkUser(user).fold(x => Left(x), { user =>
              mkPlace(place).fold(x => Left(x), { place =>
                val entities: Option[JsValue] = json.fields.get("entities")
                Right(Tweet(id, user, text, place, mkUrls(entities), mkHashTags(entities), mkPhotos(entities)))
              })
            })
            x
          case _ => Left(MalformedContent("bad tweet"))
        }
      }
    }.getOrElse(Left(MalformedContent("bad json")))
  }
}

object TweetStreamerActor {
  val twitterUri = Uri("https://stream.twitter.com/1.1/statuses/sample.json")
}

class TweetStreamerActor(uri: Uri, processor: ActorRef) extends Actor with TweetMarshaller {
  this: TwitterAuthorization =>
  val io = IO(Http)(context.system)

  def receive: Receive = {
    case BeginStreaming =>
      val rq = HttpRequest(HttpMethods.GET, uri = uri) ~> authorize
      sendTo(io).withResponsesReceivedBy(self)(rq)
    case ChunkedResponseStart(_) =>
    case MessageChunk(entity, _) => TweetUnmarshaller(entity).fold(_ => (), processor !)
    case it: Any => println(s"Unknown type: $it with class ${it.getClass.getName}")
  }
}
