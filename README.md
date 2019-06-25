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

## How

Ids are generated using 3 parts
* Timestamp (42 bits)
* Sequence (6 bits by default - can be customized)
* Instance id (16 bits by default - can be customized)

NOTE: "instance" refers to an instance of the Snowflake id generator. It does not mean machine
instance e.g. EC2 instance.

#### Timestamp
The timestamp is calculated by `now - baseTime`. Snowflake can generate ids for baseTime + 69 years.
When choosing a `baseTime` for your application choose a recent timestamp.

The baseTime **MUST NOT** be changed after deploying the system. If it is changed, guarantees
regarding ids are forfeit.

#### Sequence
Sequence is used as a tiebreaker for ids generated at the same timestamp for the same Snowflake
instance. By default the sequence is a counter which is incremented for timestamps generated at the
same millisecond. 

TODO: Link to issue about using random sequences

#### Instance ID
The instance ID uniquely identifies a Snowflake instance. To ensure uniqueness, each active instance
of Snowflake must have a unique id.

This can be chosen in various ways, including
* Deriving an id from something that is unique per instance (e.g. IP address, MAC address)
* A random number (and hope that the same random number is not generated)

## Usage
```java
Snowflake snowflake = Snowflake.builder()
    // Specify a custom baseTime (MUST NOT be changed after deploying)
    .withBaseTime(1_560_939_370_000L) // 2019-06-19 10:16:10 UTC
    .withId(123)
    .build();
long id = snowflake.generate();
```

## License
Licensed under the MIT license because Camflake was used as the basis for this project and that is the license Camflake uses.

## Further Reading
* https://web.archive.org/web/20190614171752/https://blog.twitter.com/engineering/en_us/a/2010/announcing-snowflake.html
* https://rob.conery.io/2014/05/28/a-better-id-generator-for-postgresql/

### What was wrong with Camflake?
* It can't be unit tested without mocking (ew)
* A [couple of minor bugs](https://github.com/cam-inc/camflake/issues/created_by/yaseenkadir)

## Differences to Camflake
* No id retrying
  * If the sequence is exhausted we do not try to sleep again. An exception will be thrown. Clients can choose what to do in that scenario. Given that a sequence can only be exhausted if you generate 64 ids in the same **millisecond** it seems quite unlikely to happen to most people. And if you don't catch it an occassional 5xx is probably okay.
* Lets you provide a Clock (helpful for testing)
* A few bug fixes and documentation improvements
