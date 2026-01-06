package org.fim.wallet.domain.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.Serializable
import org.fim.wallet.domain.serialize.CircuitInputsSerializer

/**
 * Auxiliary class to wrap inputs for circom circuits.
 *
 * Since circuits expect a JS Object of key-value paris, where the values may be BigIntegers or an Array of BigIntegers,
 * a wrapper class is needed to reflect that behavior in Kotlin.
 * So this class combines to maps, one for BigInteger pairs and one for the Array pairs into one object.
 */
@Serializable(CircuitInputsSerializer::class)
data class CircuitInputs(
  val inputs: List<Pair<String, BigInteger>>? = null,
  val arrayInputs: List<Pair<String, List<BigInteger>>>? = null,
) {
  init {
    require(arrayInputs != null || inputs != null) {
      "At least one input must be provided."
    }
  }

  /**
   * Combines two [CircuitInputs] instances by concatenating their respective inputs and arrayInputs.
   *
   * @param b The [CircuitInputs] instance to add to this instance
   * @return A new [CircuitInputs] containing the combined inputs from both instances
   */
  operator fun plus(b: CircuitInputs): CircuitInputs {
    val newInputs = inputs?.plus(b.inputs ?: emptyList()) ?: b.inputs
    val newArrayInputs = arrayInputs?.plus(b.arrayInputs ?: emptyList()) ?: b.arrayInputs

    return CircuitInputs(newInputs, newArrayInputs)
  }
  /**
   * Total number of values in [inputs] and [arrayInputs].
   */
  val numberOfInputs: Int get() = (inputs?.size ?: 0) + (arrayInputs?.sumOf { it.second.size }?: 0)
}
