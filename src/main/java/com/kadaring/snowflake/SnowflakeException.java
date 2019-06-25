package com.kadaring.snowflake;

/**
 * Base exception thrown by this library
 */
public class SnowflakeException extends RuntimeException {

  public SnowflakeException(String message) {
    super(message);
  }
}
