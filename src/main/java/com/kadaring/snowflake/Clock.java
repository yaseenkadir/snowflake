package com.kadaring.snowflake;

/**
 * A clock providing access to the current time
 *
 * @implSpec Implementations must be fully thread safe
 */
@FunctionalInterface
public interface Clock {

  /**
   * Returns current time as millis since UTC Epoch
   *
   * <p>If time appears to go backwards, {@link Snowflake} will set elapsed time to 0. This is done
   * to ensure that generated ids are always in ascending order.
   *
   * <p>The default clock implementation uses {@link System#currentTimeMillis()}.
   */
  long now();
}
