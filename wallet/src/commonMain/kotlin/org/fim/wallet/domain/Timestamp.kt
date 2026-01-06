package org.fim.wallet.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fim.wallet.domain.serialize.TimestampSerializer
import kotlin.random.Random

/**
 * A value class representing a timestamp in milliseconds since January 1, 1970 (Unix epoch).
 * Encapsulates a [Long] value to ensure immutability and type safety, while enforcing
 * positive values upon creation.
 */
@Serializable(TimestampSerializer::class)
value class Timestamp(val value: Long) : Comparable<Timestamp> {

  init {
    require(value >= 0) { "Timestamp value must be positive." }
  }

  /**
   * Converts this [Timestamp] object into a JSON string.
   *
   * @return A JSON string representing this timestamp.
   */
  fun toJson(): String {
    return Json.encodeToString(this)
  }

  /**
   * Formats the [Timestamp] into a human-readable string.
   * Uses UTC by default.
   *
   * @return A string representation of the timestamp in the "YYYY-MM-DD HH:mm:ss" format.
   */
  fun toHumanReadable(): String {
    val instant = Instant.fromEpochMilliseconds(value)
    val localDateTime = instant.toLocalDateTime(TimeZone.UTC)

    val year = localDateTime.year
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val hours = localDateTime.hour.toString().padStart(2, '0')
    val minutes = localDateTime.minute.toString().padStart(2, '0')
    val seconds = localDateTime.second.toString().padStart(2, '0')

    return "$year-$month-$day $hours:$minutes:$seconds"
  }

  /**
   * Compares this [Timestamp] with another.
   * Returns:
   *  - Negative if this < other
   *  - Zero if this == other
   *  - Positive if this > other
   */
  override fun compareTo(other: Timestamp): Int {
    return value.compareTo(other.value)
  }

  /**
   * Operator overloading for `+` to add milliseconds to a [Timestamp].
   *
   * @throws IllegalArgumentException if resulting [Timestamp] would be negative.
   */
  operator fun plus(milliseconds: Long): Timestamp {
    require(value + milliseconds >= 0) { "Resulting timestamp must be positive." }
    return Timestamp(value + milliseconds)
  }

  /**
   * Operator overloading for `-` to subtract milliseconds from a [Timestamp].
   */
  operator fun minus(milliseconds: Long): Timestamp {
    require(milliseconds <= value) { "Resulting timestamp must be positive." }
    return Timestamp(value - milliseconds)
  }

  /**
   * Operator overloading for `-` to subtract one [Timestamp] from another.
   */
  operator fun minus(other: Timestamp): Timestamp {
    return Timestamp(value - other.value)
  }

  /**
   * Operator overloading for `*` to multiply a timestamp by a scalar.
   */
  operator fun times(factor: Long): Timestamp {
    require(factor >= 0) { "Multiplication factor must be positive." }
    return Timestamp(value * factor)
  }

  companion object {
    /**
     * Creates a [Timestamp] instance representing the current time in milliseconds
     * since January 1, 1970 (Unix epoch).
     *
     * @return A [Timestamp] representing the current time.
     */
    fun now(): Timestamp {
      return Timestamp(Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Generates a random [Timestamp] within a given range.
     *
     * @param from The start timestamp (inclusive).
     * @param until The end timestamp (exclusive).
     *
     * @return A random [Timestamp] between [from] and [until].
     * @throws IllegalArgumentException if [from] is greater than [until].
     */
    fun random(from: Long = 0, until: Long = Long.MAX_VALUE): Timestamp {
      require(from < until) { "Invalid range: from must be less than until." }
      return Timestamp(Random.nextLong(from, until))
    }

    /**
     * Converts a JSON string into a [Timestamp] object.
     *
     * @param json The JSON string representing a timestamp.
     * @return A [Timestamp] object.
     * @throws IllegalArgumentException if the JSON is invalid or deserialization fails.
     */
    fun fromJson(json: String): Timestamp {
      return Json.decodeFromString(json)
    }
  }
}
