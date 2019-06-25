package com.kadaring.snowflake;

/**
 * Tells the current time
 *
 * @implSpec Implementations must be fully thread safe
 */
@FunctionalInterface
public interface Clock {

  /**
   * Returns current time as millis since UTC Epoch
   */
  long now();
}
