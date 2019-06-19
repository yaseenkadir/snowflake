# Snowflake
Another Snowflake implementation in Java. Based off of a few other projects
* Twitter Snowflake (the OG one)
* Sonyflake
* Camflake

This library uses Camflake as basis but with a few minor changes.

## Why?
* Because Twitter Snowflake is in Scala and I couldn't understand it
* Sonyflake is written in Go
* Camflake is very good, there are a couple of minor bugs in it though that I wanted to address.
* I hadn't seen [this blog](https://www.callicoder.com/distributed-unique-id-sequence-number-generator/) or its [corresponding repo](https://www.callicoder.com/distributed-unique-id-sequence-number-generator/) before I decided to make my own. If I had I wouldn't have created this one.

### What was wrong with Camflake?
* It can't be unit tested without mocking (ew)
* A [couple of minor bugs](https://github.com/cam-inc/camflake/issues/created_by/yaseenkadir)

## License
Licensed under the MIT license because Camflake was used as the basis for this project and that is the license Camflake uses.

## Differences to Camflake
* No id retrying
  * If the sequence is exhausted we do not try to sleep again. An exception will be thrown. Clients can choose what to do in that scenario. Given that a sequence can only be exhausted if you generate 64 ids in the same **millisecond** it seems quite unlikely to happen to most people. And if you don't catch it an occassional 5xx is probably okay.
* Lets you provide a Clock (helpful for testing)
* A few bug fixes and documentation improvements

## Further Reading
* https://web.archive.org/web/20190614171752/https://blog.twitter.com/engineering/en_us/a/2010/announcing-snowflake.html
* https://rob.conery.io/2014/05/28/a-better-id-generator-for-postgresql/
