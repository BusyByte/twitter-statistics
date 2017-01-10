package net.nomadicalien.twitter

import net.nomadicalien.twitter.models.{ApplicationError, MissingConfigError}

trait Config {
  def maybeConsumerKey: Either[ApplicationError, String]
  def maybeConsumerSecret: Either[ApplicationError, String]
  def maybeAccessToken: Either[ApplicationError, String]
  def maybeAccessTokenSecret: Either[ApplicationError, String]
}


trait EnvVariableConfig extends Config {
  def getEnvVar(envVarName: String): Either[ApplicationError, String]

  def maybeConsumerKey = getEnvVar("TWITTER_CONSUMER_KEY")
  def maybeConsumerSecret = getEnvVar("TWITTER_CONSUMER_SECRET")
  def maybeAccessToken = getEnvVar("TWITTER_ACCESS_TOKEN")
  def maybeAccessTokenSecret = getEnvVar("TWITTER_ACCESS_TOKEN_SECRET")
}

object EnvVariableConfig extends EnvVariableConfig {
  def getEnvVar(envVarName: String) = scala.sys.env.get(envVarName)
    .fold[Either[ApplicationError, String]](Left(MissingConfigError(s"$envVarName environment variable is missing")))(Right.apply)
}





