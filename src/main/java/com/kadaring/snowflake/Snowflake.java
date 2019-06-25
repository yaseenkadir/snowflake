package com.kadaring.snowflake;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique ids.
 *
 * Snowflake provides the following guarantees
 * <ul>
 * <li>ids can be generated on many nodes without coordination between nodes</li>
 * <li>ids are unique</li>
 * <li>ids across multiple instances are roughly in increasing order</li>
 * </ul>
 *
 * This guarantees are only possible if all the following conditions are met
 * <ul>
 * <li>The id uniquely identifies an instance of this class</li>
 * <li>{@link Clock}s are in sync across all instances</li>
 * </ul>
 *
 * <p>An id is made up of three components
 * <ul>
 * <li>Millis since base time (42 bits)</li>
 * <li>Sequence (6 bits by default - can be customized)</li>
 * <li>Machine ID (16 bits by default - can be customized)</li>
 * </ul>
 */
public class Snowflake {

  /**
   * Maximum elapsed time from base time. 2,199,023,255,551 milliseconds after base time. This is 69
   * years.
   *
   * <p>We shift it over 41 bits instead of 42 because we want to prevent overflow issues. If we
   * shifted by 42 bits, generated ids can become negative.
   */
  static final long MAX_TIME_MILLIS = (1L << 41) - 1L;

  private final int id;
  private final Clock clock;
  private final long baseTime;
  private final int idBits;
  private final int maxSequence;
  private final Object lock = new Object();
  private final AtomicInteger sequence = new AtomicInteger();
  private volatile long lastSequenceTimestamp;

  private Snowflake(Builder builder) {
    this.baseTime = builder.baseTime;
    this.id = builder.id;
    this.clock = builder.clock;
    this.idBits = builder.idBits;
    this.maxSequence = (1 << builder.sequenceBits) - 1;
  }

  public long generate() {
    long now = clock.now();
    long elapsedTime = getElapsedTime(now);
    long sequence = getSequence(now);
    return (elapsedTime << 22) | (sequence << idBits) | id;
  }

  private long getElapsedTime(long now) {
    long elapsed = now - baseTime;
    if (elapsed > MAX_TIME_MILLIS) {
      // RIP to the person who sees this exception in prod years after this code is deployed
      throw new SnowflakeException("Exceeded the time limit");
    }
    return elapsed;
  }

  private int getSequence(long now) {
    int nextSequence;
    // TODO Would it be better to use an atomic reference?
    synchronized (lock) {
      if (lastSequenceTimestamp < now) {
        lastSequenceTimestamp = now;
        sequence.set(0);
      }
      nextSequence = sequence.getAndIncrement();
    }
    if (nextSequence > maxSequence) {
      throw new SnowflakeException(String.format("Exceeded max sequence %s", maxSequence));
    }
    return nextSequence;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private static final Clock DEFAULT_CLOCK = System::currentTimeMillis;
    static final int DEFAULT_SEQUENCE_BITS = 6;
    static final int DEFAULT_ID_BITS = 16;
    // visible for testing
    static final int REQUIRED_SEQUENCE_AND_ID_BITS =
        DEFAULT_ID_BITS + DEFAULT_SEQUENCE_BITS;

    // Using boxed primitives so that we can just do null checks to see if they've been set
    private Long baseTime;
    private Integer id;
    private Clock clock;
    private Integer sequenceBits;
    private Integer idBits;

    Builder() {
      this(null, null);
    }

    /**
     * Convenience builder for required params.
     *
     * <pre>
     * {@code
     * new Snowflake.Builder(0, 123).build();
     * // vs
     * new Snowflake.Builder()
     *     .withBaseTime(0)
     *     .withId(123)
     *     .build();
     * }
     * </pre>
     *
     * @param baseTime base time used to calculate the elapsed time. Millis since UTC epoch.
     * @param id uniquely identifies this Snowflake instance
     */
    Builder(Long baseTime, Integer id) {
      this.baseTime = baseTime;
      this.id = id;
      this.clock = DEFAULT_CLOCK;
      this.sequenceBits = null;
      this.idBits = null;
    }

    /**
     * @param baseTime base time used to calculate the elapsed time. Millis since UTC epoch.
     */
    public Builder withBaseTime(Long baseTime) {
      this.baseTime = baseTime;
      return this;
    }

    /**
     * @param id uniquely identifies this Snowflake instance
     */
    public Builder withId(Integer id) {
      this.id = id;
      return this;
    }

    /**
     * @param clock clock to use in order to get current time, default clock uses {@link
     * System#currentTimeMillis()}
     */
    public Builder withClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * @param idBits number of bits to use for the snowflake id component of the generated id
     */
    public Builder withIdBits(Integer idBits) {
      this.idBits = idBits;
      return this;
    }

    /**
     * @param sequenceBits number of bits to use for the sequence component of the generated id
     */
    public Builder withSequenceBits(Integer sequenceBits) {
      this.sequenceBits = sequenceBits;
      return this;
    }

    public Snowflake build() {
      Objects.requireNonNull(baseTime, "baseTime must be set");
      Objects.requireNonNull(id, "id must be set");
      sequenceBits = (sequenceBits == null ? DEFAULT_SEQUENCE_BITS : sequenceBits);
      idBits = (idBits == null ? DEFAULT_ID_BITS : idBits);

      checkNonNegative(baseTime, "baseTime");
      checkNonNegative(id, "id");
      checkNonNegative(idBits, "idBits");
      checkNonNegative(sequenceBits, "sequenceBits");

      if (clock.now() < baseTime) {
        throw new IllegalArgumentException("baseTime is in the future");
      }

      if (sequenceBits + idBits != REQUIRED_SEQUENCE_AND_ID_BITS) {
        throw new IllegalArgumentException(
            "sequence and id bits must sum to " + REQUIRED_SEQUENCE_AND_ID_BITS);
      }
      checkIdBits();
      return new Snowflake(this);
    }

    private static void checkNonNegative(long value, String field) {
      if (value < 0) {
        throw new IllegalArgumentException(String.format("%s must be non-negative", field));
      }
    }

    private void checkIdBits() {
      if (idBits == 0) {
        if (id != 0) {
          throw new IllegalArgumentException("id must be 0 if idBits is 0");
        }
      } else {
        checkIdFitsInIdBits();
      }
    }

    private void checkIdFitsInIdBits() {
      if (id >= (1 << idBits)) {
        throw new IllegalArgumentException(
            String.format("id %s exceeds max possible value for %s id bits", id, idBits));
      }
    }
  }
}
