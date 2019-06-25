package com.kadaring.snowflake;

import static com.kadaring.snowflake.Snowflake.Builder.DEFAULT_ID_BITS;
import static com.kadaring.snowflake.Snowflake.Builder.DEFAULT_SEQUENCE_BITS;
import static com.kadaring.snowflake.Snowflake.Builder.REQUIRED_SEQUENCE_AND_ID_BITS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

public class SnowflakeTest {

  private static final Clock EPOCH_CLOCK = () -> 0;
  private static final int DEFAULT_MAX_ID = (1 << DEFAULT_ID_BITS) - 1;

  @Test
  public void testIdsIncrease() {
    Snowflake snowflake = createSnowflakeWithEpochClock();

    long id1 = snowflake.generate();
    long id2 = snowflake.generate();

    assertThat(id2).isGreaterThan(id1);
  }

  @Test
  public void testIdOrderingIsNotGuaranteedAtSameTimestamp() {
    // This test is more of an example as to why we can't guarantee exact ordering

    Snowflake s1 = Snowflake.builder()
        .withId(0)
        .withBaseTime(0L)
        .withClock(EPOCH_CLOCK)
        .build();

    Snowflake s2 = Snowflake.builder()
        .withId(1)
        .withBaseTime(0L)
        .withClock(EPOCH_CLOCK)
        .build();

    // s2Id2 is generated first but is greater than s1Id1
    // At the same timestamp, ordering is not guaranteed
    long s2Id2 = s2.generate();
    long s1Id1 = s1.generate();

    assertThat(s2Id2).isGreaterThan(s1Id1);
  }

  @Test
  public void testSequenceResetsAsTimeIncreases() {
    FakeClock clock = new FakeClock();
    Snowflake snowflake = createSnowflakeWithClock(clock);

    long id = snowflake.generate();
    assertThat(DeconstructedId.of(id)).isEqualTo(DeconstructedId.ZERO);
    id = snowflake.generate();
    assertThat(DeconstructedId.of(id)).isEqualTo(new DeconstructedId(0, 1, 0));

    clock.millis++;

    id = snowflake.generate();
    assertThat(DeconstructedId.of(id)).isEqualTo(new DeconstructedId(1, 0, 0));
  }

  @Test
  public void testMinId() {
    Snowflake snowflake = Snowflake.builder()
        .withBaseTime(0L)
        .withId(0)
        .withClock(EPOCH_CLOCK)
        .build();
    long id = snowflake.generate();
    assertThat(id).isEqualTo(0);
  }

  @Test
  public void testMaxId() {
    Clock clock = () -> Snowflake.MAX_TIME_MILLIS;
    Snowflake snowflake = Snowflake.builder()
        .withBaseTime(0L)
        .withId(DEFAULT_MAX_ID)
        .withClock(clock)
        .build();
    long id = generateIdsNTimes(snowflake, 1 << DEFAULT_SEQUENCE_BITS);
    assertThat(id).isEqualTo(Long.MAX_VALUE);
    assertThat(DeconstructedId.of(id))
        .isEqualTo(new DeconstructedId(Snowflake.MAX_TIME_MILLIS, (1 << DEFAULT_SEQUENCE_BITS) - 1,
            DEFAULT_MAX_ID));
  }

  @Test
  public void testIncrementsSequenceNumberForSameTimestamp() {
    Snowflake snowflake = Snowflake.builder()
        .withId(0)
        .withClock(EPOCH_CLOCK)
        .withBaseTime(0L)
        .build();

    long id1 = snowflake.generate();
    long id2 = snowflake.generate();
    long id3 = snowflake.generate();

    assertThat(DeconstructedId.of(id1)).isEqualTo(DeconstructedId.ZERO);
    assertThat(DeconstructedId.of(id2)).isEqualTo(new DeconstructedId(0, 1, 0));
    assertThat(DeconstructedId.of(id3)).isEqualTo(new DeconstructedId(0, 2, 0));
  }

  @Test
  public void testThrowsAfterMaxSequenceAttempts() {
    Snowflake snowflake = createSnowflakeWithEpochClock();
    generateIdsNTimes(snowflake, 64);
    Exception e = assertThrows(SnowflakeException.class, snowflake::generate);
    assertThat(e.getMessage()).isEqualTo("Exceeded max sequence 63");
  }

  @Test
  public void throwsIfMaxTimeExceeded() {
    Clock clock = () -> Snowflake.MAX_TIME_MILLIS + 1;
    Snowflake snowflake = createSnowflakeWithClock(clock);
    Exception e = assertThrows(SnowflakeException.class, snowflake::generate);
    assertThat(e.getMessage()).isEqualTo("Exceeded the time limit");
  }

  @Test
  public void testTimestampIsLeadingBits() {
    Clock clock = () -> 1;
    Snowflake snowflake = createSnowflakeWithClock(clock);
    long id = snowflake.generate();
    assertEquals(4194304, id);
    assertEquals("10000000000000000000000", Long.toBinaryString(id));

    clock = () -> Snowflake.MAX_TIME_MILLIS;
    snowflake = createSnowflakeWithClock(clock);
    id = snowflake.generate();
    assertEquals("111111111111111111111111111111111111111110000000000000000000000",
        Long.toBinaryString(id));
  }

  @Test
  public void testMinIdBitsAndMaxSequenceBits() {
    Clock clock = () -> 1;
    Snowflake snowflake = Snowflake.builder()
        .withIdBits(0)
        .withId(0) // if idBits is 0, id must be 0
        .withBaseTime(0L)
        .withClock(clock)
        .withSequenceBits(22)
        .build();

    long id = snowflake.generate();
    assertThat(Long.toBinaryString(id)).isEqualTo("10000000000000000000000");

    id = generateIdsNTimes(snowflake, (1 << REQUIRED_SEQUENCE_AND_ID_BITS) - 1);
    assertThat(Long.toBinaryString(id)).isEqualTo("11111111111111111111111");
  }

  @Test
  public void testMinSequenceBitsAndMaxIdBits() {
    Clock clock = () -> 1;
    Snowflake snowflake = Snowflake.builder()
        .withId(1)
        .withIdBits(REQUIRED_SEQUENCE_AND_ID_BITS)
        .withSequenceBits(0)
        .withBaseTime(0L)
        .withClock(clock)
        .build();

    long id = snowflake.generate();
    assertThat(Long.toBinaryString(id)).isEqualTo("10000000000000000000001");

    Exception e = assertThrows(SnowflakeException.class, snowflake::generate);
    assertThat(e.getMessage()).isEqualTo("Exceeded max sequence 0");
  }

  @Test
  public void testCustomSequenceAndIdBits() {
    Clock clock = () -> 1;
    Snowflake snowflake = Snowflake.builder()
        .withId(1)
        .withIdBits(19)
        .withSequenceBits(3)
        .withBaseTime(0L)
        .withClock(clock)
        .build();

    long id = generateIdsNTimes(snowflake, 2);
    assertThat(Long.toBinaryString(id)).isEqualTo("10010000000000000000001");
    generateIdsNTimes(snowflake, 6);

    Exception e = assertThrows(SnowflakeException.class, snowflake::generate);
    assertThat(e.getMessage()).isEqualTo("Exceeded max sequence 7");
  }

  private static long generateIdsNTimes(Snowflake snowflake, int times) {
    for (int i = 0; i < times - 1; i++) {
      snowflake.generate();
    }
    return snowflake.generate();
  }

  private static Snowflake createSnowflakeWithEpochClock() {
    return createSnowflakeWithClock(EPOCH_CLOCK);
  }

  private static Snowflake createSnowflakeWithClock(Clock clock) {
    return Snowflake.builder()
        .withId(0)
        .withClock(clock)
        .withBaseTime(0L)
        .build();
  }

  private static class FakeClock implements Clock {

    long millis = 0;

    @Override
    public long now() {
      return millis;
    }
  }

  private static class DeconstructedId {

    static final DeconstructedId ZERO = new DeconstructedId(0, 0, 0);

    final long timestamp;
    final int sequence;
    final int id;

    DeconstructedId(long elapsed, int sequence, int id) {
      this.timestamp = elapsed;
      this.sequence = sequence;
      this.id = id;
    }

    static DeconstructedId of(long id) {
      // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx|xxxxxx|xxxxxxxxxxxxxxxx
      // timestamp                                | seq  | id

      // To get the individual parts we perform shifts and bitwise & operations.
      // E.g. to get the id we
      // 010101010101010101010101010101010101010101|010101|0101010101010101 AND
      //                                                   1111111111111111
      // ------------------------------------------------------------------
      //                                                   0101010101010101
      int idComponent = (int) id & ((1 << 16) - 1);
      int sequence = (int) (id >> 16) & ((1 << 6) - 1);
      long timestamp = (id >> 22) & ((1L << 42) - 1);
      return new DeconstructedId(timestamp, sequence, idComponent);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DeconstructedId that = (DeconstructedId) o;

      if (timestamp != that.timestamp) {
        return false;
      }
      if (sequence != that.sequence) {
        return false;
      }
      return id == that.id;
    }

    @Override
    public int hashCode() {
      int result = (int) (timestamp ^ (timestamp >>> 32));
      result = 31 * result + sequence;
      result = 31 * result + id;
      return result;
    }

    @Override
    public String toString() {
      return "DeconstructedId{" +
          "timestamp=" + timestamp +
          ", sequence=" + sequence +
          ", id=" + id +
          '}';
    }
  }
}
