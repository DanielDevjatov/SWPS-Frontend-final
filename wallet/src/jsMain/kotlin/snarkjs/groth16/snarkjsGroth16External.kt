@file:JsModule("snarkjs")
@file:JsQualifier("groth16")
@file:JsNonModule

package snarkjs.groth16
import org.fim.wallet.domain.zkp.CircomResult
import kotlin.js.Promise

/**
 * External function of SnarkJS to generate a full ZKP.
 * This function will generate the witness itself, so the raw input JSON object is required here.
 *
 * This function is async will return a [Promise].
 *
 * @param input         JSON object with input names as [String] and the input values as BigInteger
 * @param wasmFile      full path to the pre-compiled wasm file of the circuit that is to be used for this proof
 * @param zKeyFileName  full path to the zKey-file of the circuit that is to be used
 * @param logger        optional logger instance of [logplease.create]
 *
 * @return JSON object of the circuits public signals and the proof that can be parsed to a [CircomResult]
 */
external fun fullProve(input: dynamic, wasmFile: String, zKeyFileName: String, logger: dynamic): Promise<Any>

/**
 * External function of SnarkJS to verify a proof.
 *
 * This function is async will return a [Promise].
 *
 * @param vkVerifier    the verification key as a JSON object (not as a file)
 * @param publicSignals array of the public signals
 * @param proof         JSON object of the proof
 * @param logger        optional logger instance of [logplease.create]
 *
 * @return true if proof is valid for a given verification key and signals, false otherwise
 */
external fun verify(vkVerifier: dynamic, publicSignals: dynamic, proof: dynamic, logger: dynamic): Promise<Boolean>
