package org.fim.wallet.application.port.out

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.zkp.CircomResult
import org.fim.wallet.domain.zkp.CircuitInputs
import org.fim.wallet.domain.zkp.Proof

/**
 * Port interface definition for ZKP generation and verification.
 */
interface ZKPProvider {

  /**
   * Create a proof for given circuit and input signals.
   * The inputs must be cast to [BigInteger] beforehand.
   *
   * @param circuitName the name of the circuit that should be used
   * @param inputs      input signals for the circuit as [CircuitInputs] wrapper
   *
   * @return a [CircomResult] for given circuit and input signals
   *
   * @throws [NoSuchElementException] if no circuit could be found with given name
   */
  suspend fun fullProve(
    circuitName: String,
    inputs: CircuitInputs
  ): CircomResult

  /**
   * Verify a proof for given circuit and public signals.
   * The public signals must start with the circuit outputs followed by the public inputs.
   * All public signals must be provided in the same order as provided from the circuit at proof generation.
   *
   * No specific exceptions will be thrown if a different circuit is used for proof verification than for proof generation.
   * The same holds for public signals that don't match the proof.
   * In both cases the function will return false without exceptions.
   *
   * @param circuitName   the name of the circuit the proof was generated with
   * @param publicSignals list of public signals returned at proof generation
   * @param proof         the proof to verify
   *
   * @return true if the proof is valid, false otherwise
   *
   * @throws [NoSuchElementException] if no circuit could be found with given name
   */
  suspend fun verify(
    circuitName: String,
    publicSignals: List<BigInteger>,
    proof: Proof
  ): Boolean
}
