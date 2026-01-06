package org.fim.wallet.domain

import kotlinx.serialization.json.Json
import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TimestampTest {
  @Test
  fun testValidTimestampInitializationWithLong() {
    val timestamp = Timestamp(1000L)
    assertEquals(1000L, timestamp.value, "The timestamp value should be 1000.")
  }

  @Test
  fun testValidTimestampInitializationWithInt() {
    val timestamp = Timestamp(1000)
    assertEquals(1000L, timestamp.value, "The timestamp value should be 1000 (converted from Int).")
  }

  @Test
  fun testInvalidTimestampInitializationWithNegativeValue() {
    assertFailsWith<IllegalArgumentException>("Timestamp value must be positive.") {
      Timestamp(-1000L)
    }
  }

  @Test
  fun testNowMethodReturnsValidTimestamp() {
    val now = Timestamp.now()
    val currentTime = Date.now().toLong()

    assertTrue(now.value <= currentTime, "Timestamp.now() should not exceed the current time.")
    assertTrue(now.value > currentTime - 1000, "Timestamp.now() should be close to the current time.")
  }

  @Test
  fun testToHumanReadableReturnsCorrectFormat() {
    val testTimestamp = Timestamp(1676560215000L) // Fixed value for testing: 2023-02-16 01:30:15 UTC
    val humanReadable = testTimestamp.toHumanReadable()
    assertEquals("2023-02-16 15:10:15", humanReadable, "The human-readable format should match.")
  }

  @Test
  fun testToHumanReadableWithSingleDigitDayAndTimeValues() {
    val testTimestamp = Timestamp(1672531200000L) // 2023-01-01 00:00:00 UTC
    val humanReadable = testTimestamp.toHumanReadable()
    assertEquals("2023-01-01 00:00:00", humanReadable, "Single-digit values should be properly padded.")
  }

  @Test
  fun testMultipleTimestampsAreUnique() {
    val timestamp1 = Timestamp.now()
    val timestamp2 = Timestamp.now()
    assertTrue(
      timestamp2.value >= timestamp1.value,
      "Later timestamps should be greater than or equal to earlier ones."
    )
  }

  @Test
  fun testTimestampWithVeryLargeValues() {
    val timestamp = Timestamp(Long.MAX_VALUE)
    assertEquals(Long.MAX_VALUE, timestamp.value, "The timestamp should handle large Long values.")
  }

  private val json = Json { prettyPrint = true }

  @Test
  fun testSerialization() {
    // Create a timestamp with a fixed value
    val timestamp = Timestamp(1676560215000L) // Corresponds to 2023-02-16 01:30:15 UTC
    // Serialize the timestamp to JSON
    val serialized = timestamp.toJson()
    // Verify the JSON output
    assertEquals("1676560215000", serialized, "Serialized JSON should match the underlying value.")
  }

  @Test
  fun testDeserialization() {
    // JSON representation of the timestamp
    val jsonInput = "1676560215000"
    // Deserialize the JSON into a Timestamp object
    val deserialized = Timestamp.fromJson(jsonInput)
    // Verify the value of the deserialized timestamp
    assertEquals(1676560215000L, deserialized.value, "Deserialized value should match the JSON input.")
  }

  @Test
  fun testStaticFromJson() {
    // JSON representation of the timestamp
    val jsonInput = "1676560215000"
    // Deserialize the JSON into a Timestamp object
    val deserialized = Timestamp.fromJson(jsonInput)
    // Verify the value of the deserialized timestamp
    assertEquals(1676560215000L, deserialized.value, "Deserialized value should match the JSON input.")
  }

  @Test
  fun testRoundTripSerialization() {
    // Create a timestamp with a fixed value
    val originalTimestamp = Timestamp(1739563200000L) // Corresponds to 2025-02-14 14:00:00 UTC
    // Serialize the timestamp
    val serialized = originalTimestamp.toJson()
    // Deserialize the JSON back into a Timestamp object
    val deserialized = Timestamp.fromJson(serialized)
    // Verify the original and deserialized timestamps are equal
    assertEquals(
      originalTimestamp.value,
      deserialized.value,
      "Round-trip serialization should produce the same value."
    )
  }

  @Test
  fun testToJson() {
    // Create a timestamp with a fixed value
    val originalTimestamp = Timestamp(1739563200000L) // Corresponds to 2025-02-14 14:00:00 UTC
    // Serialize the timestamp
    val serialized = originalTimestamp.toJson()
    // Deserialize the JSON back into a Timestamp object
    val deserialized = Timestamp.fromJson(serialized)
    // Verify the original and deserialized timestamps are equal
    assertEquals(
      originalTimestamp.value,
      deserialized.value,
      "Round-trip serialization should produce the same value."
    )
  }

  @Test
  fun testInvalidDeserialization() {
    // Invalid JSON input (negative value)
    val invalidJson = "-1676560215000"
    // Attempt to deserialize and verify it throws an exception
    assertFailsWith<IllegalArgumentException> {
      json.decodeFromString(Timestamp.serializer(), invalidJson)
    }
  }

  @Test
  fun testRandomTimestampWithinDefaultRange() {
    val randomTimestamp = Timestamp.random()

    assertTrue(
      randomTimestamp.value in 0..Long.MAX_VALUE,
      "Random timestamp should be between 0 and Long.MAX_VALUE."
    )
  }

  @Test
  fun testRandomTimestampWithinCustomRange() {
    val start = Timestamp(1609459200000L) // 2021-01-01 00:00:00 UTC
    val end = Timestamp(1672444800000L)   // 2023-01-01 00:00:00 UTC

    val randomTimestamp = Timestamp.random(start.value, end.value)

    assertTrue(
      randomTimestamp.value in start.value..end.value,
      "Random timestamp should be between 2021-01-01 and 2023-01-01."
    )
  }

  @Test
  fun testRandomTimestampProducesDifferentResults() {
    val timestamp1 = Timestamp.random()
    val timestamp2 = Timestamp.random()

    assertTrue(
      timestamp1.value != timestamp2.value,
      "Two consecutive calls to Timestamp.random() should produce different values most of the time."
    )
  }

  @Test
  fun testInvalidRandomRangeThrowsException() {
    val start = Timestamp(1700000000000L) // Future date
    val end = Timestamp(1600000000000L)   // Past date (invalid range)

    assertFailsWith<IllegalArgumentException>(
      message = "Random timestamp generation should fail when 'from' is greater than 'to'."
    ) {
      Timestamp.random(start.value, end.value)
    }
  }

  @Test
  fun testAdditionWithLong() {
    val timestamp = Timestamp(1000L)
    val result = timestamp + 500L
    assertEquals(1500L, result.value, "Adding 500ms to 1000ms should result in 1500ms.")
  }

  @Test
  fun testAdditionWithInt() {
    val timestamp = Timestamp(1000L)
    val result = timestamp + 250
    assertEquals(1250L, result.value, "Adding 250ms (Int) should work the same as Long.")
  }

  @Test
  fun testAdditionWithToLargeNegativeThrowsException() {
    val timestamp = Timestamp(1000L)
    assertFailsWith<IllegalArgumentException>("Cannot add negative milliseconds.") {
      timestamp + (-1100L)
    }
  }

  @Test
  fun testSubtractionWithLong() {
    val timestamp = Timestamp(2000L)
    val result = timestamp - 1000L
    assertEquals(1000L, result.value, "Subtracting 1000ms from 2000ms should result in 1000ms.")
  }

  @Test
  fun testSubtractionWithInt() {
    val timestamp = Timestamp(2000L)
    val result = timestamp - 500
    assertEquals(1500L, result.value, "Subtracting 500ms (Int) should work correctly.")
  }

  @Test
  fun testSubtractionResultsInPositiveOnly() {
    val timestamp = Timestamp(1000L)
    assertFailsWith<IllegalArgumentException>("Resulting timestamp must be positive.") {
      timestamp - 2000L
    }
  }

  @Test
  fun testSubtractionBetweenTimestamps() {
    val t1 = Timestamp(3000L)
    val t2 = Timestamp(1000L)
    val difference = t1 - t2
    assertEquals(2000L, difference.value, "Difference between timestamps should be absolute.")
  }

  @Test
  fun testMultiplicationWithLong() {
    val timestamp = Timestamp(1000L)
    val result = timestamp * 3L
    assertEquals(3000L, result.value, "Multiplying 1000ms by 3 should result in 3000ms.")
  }

  @Test
  fun testMultiplicationWithInt() {
    val timestamp = Timestamp(1000L)
    val result = timestamp * 2
    assertEquals(2000L, result.value, "Multiplying 1000ms by 2 should result in 2000ms.")
  }

  @Test
  fun testMultiplicationWithZeroThrowsException() {
    val timestamp = Timestamp(1000L)
    assertFailsWith<IllegalArgumentException>("Multiplication factor must be positive.") {
      timestamp * -1
    }
  }

  @Test
  fun testComparisonOperators() {
    val t1 = Timestamp(1000L)
    val t2 = Timestamp(2000L)

    assertTrue(t1 < t2, "t1 should be less than t2.")
    assertTrue(t2 > t1, "t2 should be greater than t1.")
    assertEquals(t1, Timestamp(1000L), "t1 should be equal to a timestamp with the same value.")
  }
}
