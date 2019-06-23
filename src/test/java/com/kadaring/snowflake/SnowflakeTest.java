package com.kadaring.snowflake;

import org.junit.Test;

public class SnowflakeTest {
  private static final Clock EPOCH_CLOCK = () -> 0;

  @Test
  public void buildDefaultInstance() {
    Snowflake.Builder builder = new Snowflake.Builder();
    // Should not throw
    Snowflake snowflake = builder.withBaseTime(0L)
        .withId(123)
        .build();
  }
}
