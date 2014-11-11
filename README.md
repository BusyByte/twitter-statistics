twitter-statistics
==================

#Spray client, Akka and streaming tweets

Twitter streaming & simple sentiment analysis application. To build & run the plain-vanilla version of the application, run ``sbt run``.
Performs analyisis of the random sample of tweets from "https://stream.twitter.com/1.1/statuses/sample.json

Metrics Reported:
total number of tweets received
average tweets per hour/minute/second
top 10 emojis
percent of tweets w/ an emoji
top 10 hashtags
percent of tweets w/ a url
percent of tweets w/ photo url
top 10 domains of urls in tweets


##Twitter application
Before you run the application, create the ``~/.twitter/activator`` file, containing four lines; these lines represent your twitter consumer key and secret, followed by token value and token secret. To generate these values, head over to https://dev.twitter.com/apps/, create an application and add the appropriate lines to this file. An example ``~/.twitter/activator`` is

```
*************TqOdlxA
****************************Fv9b1ELexCRhI
********-*************************GUjmnWQvZ5GwnBR2
***********************************ybgUNqrZwD
```

Naturally, the you will need to replace the ``*``s with the values in your consumer token and secret; and token value and secret.

Note: make sure you update your computer's clock otherwise you could occasionally receive a 401 unauthorized.

##Running
Having added the file above, you can see the application "in action", by run ``sbt run`` in an ANSI terminal.
