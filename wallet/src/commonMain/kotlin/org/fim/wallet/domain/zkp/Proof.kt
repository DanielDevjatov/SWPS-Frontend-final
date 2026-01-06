@file:UseSerializers(BigIntegerSerializer::class)

package org.fim.wallet.domain.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.encodeToJsonElement
import org.fim.wallet.domain.moduleJsonBuilder
import org.fim.wallet.domain.serialize.BigIntegerSerializer

/**
 * Wrapper class for the proof object returned from snarkJs.
 *
 * @property piA        externally defined array of values
 * @property piB        externally defined (nested) array of values
 * @property piC        externally defined array of values
 * @property protocol   name of the protocol used for zkp generation
 * @property curve      name of the elliptic curve used for zkp generation
 */
@Serializable
data class Proof(
  @SerialName("pi_a")
  val piA: List<BigInteger>,
  @SerialName("pi_b")
  val piB: List<List<BigInteger>>,
  @SerialName("pi_c")
  val piC: List<BigInteger>,
  val protocol: String,
  val curve: String
) {
  init {
    require(piA.size == 3) { "Array Size of Pi_A must be equal to 3" }
    require(piB.size == 3) { "Array Size of Pi_B must be equal to 3" }
    require(piB.all { it.size == 2 }) { "Array Size of all elements in Pi_B must be equal to 2" }
    require(piC.size == 3) { "Array Size of Pi_B must be equal to 3" }
  }

  /**
   * Auxiliary function to cast a [Proof] into a [kotlinx.serialization.json.JsonElement].
   * This function uses a custom [kotlinx.serialization.json.JsonBuilder] which doesn't add the "type" key to the resulting JSON.
   *
   * @return a [kotlinx.serialization.json.JsonElement] encoding the [Proof]
   *
   * @throws kotlinx.serialization.SerializationException if the given credential cannot be serialized to JSON.
   */
  fun toJson() = moduleJsonBuilder.encodeToJsonElement(this)

  companion object {
    /**
     * Create a [Proof] from a given JSON string.
     *
     * @param jsonString    The JSON string that encodes a [Proof]
     *
     * @return [Proof] instance as defined from the given string
     *
     * @throws kotlinx.serialization.SerializationException   if the given JSON element is not a valid JSON input for a [Proof]
     * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [Proof]
     */
    fun fromJson(jsonString: String): Proof {
      return moduleJsonBuilder.decodeFromString(jsonString)
    }
  }
}
