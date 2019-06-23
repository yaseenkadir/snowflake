package com.kadaring.snowflake;

/**
 * Base exception thrown by this Snowflake library
 */
public class SnowflakeException extends RuntimeException {

  public SnowflakeException(String message) {
    super(message);
  }

  public SnowflakeException(String message, Throwable cause) {
    super(message, cause);
  }
}
