package net.nomadicalien.twitter.actor

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}

object ActorModule {
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
}
