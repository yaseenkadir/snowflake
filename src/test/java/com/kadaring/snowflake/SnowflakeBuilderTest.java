package com.kadaring.snowflake;

//import org.junit.jupiter.api.Assertions.assert

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kadaring.snowflake.Snowflake.Builder;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

// Arg/param validation and whatnot has been moved to this class to keep things small
public class SnowflakeBuilderTest {

  @Test
  public void buildDefaultInstance() {
    // Should not throw
    new Builder()
        .withBaseTime(123L)
        .withId(0)
        .build();
  }

  @Test
  public void baseTimeMustBeSet() {
    Exception e = assertThrows(NullPointerException.class, () -> new Builder()
        .withId(123)
        .build());
    assertThat(e.getMessage()).isEqualTo("baseTime must be set");
  }

  @Test
  public void idMustBeSet() {
    Exception e = assertThrows(NullPointerException.class, () -> new Builder()
        .withBaseTime(0L)
        .build());
    assertThat(e.getMessage()).isEqualTo("id must be set");
  }

  @Test
  public void idMustBeNonNegative() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        .withId(-1)
        .withBaseTime(0L)
        .build());
    assertThat(e.getMessage()).isEqualTo("id must be non-negative");
  }

  @Test
  public void baseTimeMustNotBeNegative() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        .withId(0)
        .withBaseTime(-1L)
        .build());
    assertThat(e.getMessage()).isEqualTo("baseTime must be non-negative");
  }

  @Test
  public void idBitsMustBeNonNegative() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        .withId(0)
        .withBaseTime(0L)
        .withIdBits(-1)
        .build());
    assertThat(e.getMessage()).isEqualTo("idBits must be non-negative");
  }

  @Test
  public void sequenceBitsMustBeNonNegative() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        .withId(0)
        .withBaseTime(0L)
        .withSequenceBits(-1)
        .build());
    assertThat(e.getMessage()).isEqualTo("sequenceBits must be non-negative");
  }

  @Test
  public void idBitsCanBeZero() {
    new Builder()
        .withId(0)
        .withBaseTime(0L)
        .withIdBits(0)
        .withSequenceBits(22)
        .build();
  }

  @Test
  public void idCannotBeZeroWhenIdBitsAreZero() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        // 1 is a valid id normally, but if bits are zero it is not a valid id
        .withId(1)
        .withBaseTime(0L)
        .withIdBits(0)
        .withSequenceBits(22)
        .build());
    assertThat(e.getMessage()).isEqualTo("id must be 0 if idBits is 0");
  }

  @Test
  public void idDoesNotFitInIdBits() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        .withId(2)
        .withIdBits(1)
        .withSequenceBits(21)
        .withBaseTime(0L)
        .build());
    assertThat(e.getMessage()).isEqualTo("id 2 exceeds max possible value for 1 id bits");

    e = assertThrows(IllegalArgumentException.class, () -> new Builder()
        .withId(4194304) // 2^22
        .withIdBits(22)
        .withSequenceBits(0)
        .withBaseTime(0L)
        .build());
    assertThat(e.getMessage()).isEqualTo("id 4194304 exceeds max possible value for 22 id bits");
  }

  @Test
  public void bitsMustSumTo22() {
    // Negative ids bits/sums are tested in other tests
    List<Pair<Integer, Integer>> idBitsAndSequenceBits = Arrays.asList(
        Pair.of(10, 11),
        Pair.of(0, 0),
        Pair.of(1, 1),
        Pair.of(22, 1),
        Pair.of(1, 22)
    );
    for (Pair<Integer, Integer> testCase : idBitsAndSequenceBits) {
      Exception e = assertThrows(IllegalArgumentException.class, () -> new Builder()
          .withIdBits(testCase.first)
          .withSequenceBits(testCase.second)
          .withId(0)
          .withBaseTime(0L)
          .build());
      assertThat(e.getMessage()).isEqualTo("sequence and id bits must sum to 22");
    }

    new Builder()
        .withId(0)
        .withIdBits(11)
        .withSequenceBits(11)
        .withBaseTime(0L)
        .build();
  }

  // I should just written this library in kotlin. Maybe the tests?
  private static class Pair<T1, T2> {

    private final T1 first;
    private final T2 second;

    Pair(T1 first, T2 second) {
      this.first = first;
      this.second = second;
    }

    static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
      return new Pair<>(first, second);
    }
  }
}
