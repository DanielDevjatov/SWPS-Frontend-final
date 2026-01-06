package org.fim.wallet.domain.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger

/**
 * Data class wrapper for JSON object returned from SnarkJS on proof generation.
 *
 * @property publicInputs   the public inputs returned at proof generation
 * @property outputs        the outputs generated from the circuit
 * @property proof          proof object returned from the circuit
 * @property publicSignals  derived property that returns the combination of outputs and publicInputs as received from the circuit
 */
data class CircomResult (
  val publicInputs: List<BigInteger>,
  val outputs: List<BigInteger>,
  val proof: Proof
) {
  val publicSignals = outputs + publicInputs
}
