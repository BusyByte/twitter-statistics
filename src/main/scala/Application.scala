package net.nomadicalien.twitter

import net.nomadicalien.twitter.actor.ActorModule
import org.apache.logging.log4j.LogManager

object Application extends App {
  val program: Program = Program

  program.run

  ActorModule.shutdown()
  LogManager.shutdown()
}
