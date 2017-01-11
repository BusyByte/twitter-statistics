twitter-statistics
==================

### Metrics Reported
- total number of tweets received
- average tweets per hour/minute/second
- top 10 emojis
- percent of tweets w/ an emoji
- top 10 hashtags
- percent of tweets w/ a url
- percent of tweets w/ photo url
- top 10 domains of urls in tweets

### Libraries Used
- akka-http / akka-streams - connecting to and streaming tweets
- circe - parsing json into models
- koath - authorizing oath 1a
- log4j - asynchronous logging
- cats - Show type class, implicits for flatMapping Either, Monoid

### TODO
- test with specs
- test with scalacheck
- some more general cleanup and clarify
