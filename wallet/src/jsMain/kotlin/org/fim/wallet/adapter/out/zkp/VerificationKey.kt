@file:UseSerializers(BigIntegerSerializer::class)

package org.fim.wallet.adapter.out.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.coroutines.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UseSerializers
import org.fim.wallet.domain.moduleJsonBuilder
import org.fim.wallet.domain.serialize.BigIntegerSerializer

/**
 * Data class for the verification key JSON object form snarkJS.
 * On init the dimensions of the external defined properties will be checked.
 *
 * @property protocol       name of the used ZKP protocol; by now only groth16 is in use
 * @property curve          name of the used elliptic curve; by now only bn128 is in use
 * @property nPublic        number of public signals (including outputs) of the corresponding circuit
 * @property vkAlpha1       external-defined property
 * @property vkBeta2        external-defined property
 * @property vkGamma2       external-defined property
 * @property vkDelta2       external-defined property
 * @property vkAlphaBeta12  external-defined property
 * @property ic             external-defined property
 */
@Serializable
internal data class VerificationKey(
  val protocol: String,
  val curve: String,
  val nPublic: Int,
  @SerialName("vk_alpha_1")
  val vkAlpha1: List<BigInteger>,
  @SerialName("vk_beta_2")
  val vkBeta2: List<List<BigInteger>>,
  @SerialName("vk_gamma_2")
  val vkGamma2: List<List<BigInteger>>,
  @SerialName("vk_delta_2")
  val vkDelta2: List<List<BigInteger>>,
  @SerialName("vk_alphabeta_12")
  val vkAlphaBeta12: List<List<List<BigInteger>>>,
  @SerialName("IC")
  val ic: List<List<BigInteger>>
) {

  init {
    require(vkAlpha1.size == 3) { "Size of vk_alpha_1 should be 3 but was ${vkAlpha1.size}" }

    checkDimensions(vkBeta2, "vk_beta_2")
    checkDimensions(vkGamma2, "vk_gamma_2")
    checkDimensions(vkDelta2, "vk_delta_2")

    require(vkAlphaBeta12.size == 2) { "Size of vk_alpha_beta_12 should be 2 but was ${vkAlphaBeta12.size}" }
    for (dim in vkAlphaBeta12)
      checkDimensions(dim, "vk_alphabeta_12 (sub dimension)")
  }

  companion object {
    /**
     * Parse a JSON formatted String into [VerificationKey].
     * The function will throw an exception if the JSON contains key's, that don't match the [VerificationKey]s properties.
     *
     * @param jsonString   the string representation of the [VerificationKey] to parse
     *
     * @throws SerializationException   if the given String is not a valid JSON input for [VerificationKey]
     * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [VerificationKey]
     */
    fun fromJson(jsonString: String): VerificationKey = moduleJsonBuilder.decodeFromString(jsonString)

    /**
     * Exports the a [VerificationKey] from a snarkJS zKey file.
     *
     * @param filePath  full path to the zKey file
     *
     * @returns the exported [VerificationKey]
     */
    suspend fun fromZKeyFile(filePath: String): VerificationKey {
      requireFile(filePath) { "Source $filePath to load zKey not found."}
      val key = snarkjs.zKey.exportVerificationKey(filePath, null).await().asDynamic()
      return fromJson(JSON.stringify(key))
    }

    /**
     * Auxiliary function to avoid code duplication.
     * Checks the size of the given BigInteger arrays from the JSON object at creation.
     *
     * @param vector      the array to be checked
     * @param name        the name of the array for error descriptions
     * @param nDimensions number of expected dimensions of the array; 3 by default
     * @param dimSize     expected size of the dimensions; 2 by default
     *
     * @throws IllegalArgumentException if the expected sizes don't match
     */
    private fun checkDimensions(
      vector: List<List<BigInteger>>,
      name: String,
      nDimensions: Int = 3,
      dimSize: Int = 2
    ) {
      require(vector.size == nDimensions) { "Size of $name should be $nDimensions but was ${vector.size}" }
      for (dim in vector)
        require(dim.size == dimSize) { "Size of all sub-dimensions of $name should be $dimSize but was ${dim.size}" }
    }
  }
}
