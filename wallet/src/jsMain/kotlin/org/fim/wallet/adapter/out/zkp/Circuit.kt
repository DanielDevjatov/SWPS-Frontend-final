package org.fim.wallet.adapter.out.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.json.encodeToDynamic
import node.buffer.BufferEncoding
import node.fs.existsSync
import node.fs.readFileSync
import org.fim.wallet.domain.moduleJsonBuilder
import org.fim.wallet.domain.zkp.CircomResult
import org.fim.wallet.domain.zkp.CircuitInputs
import org.fim.wallet.domain.zkp.Proof

/**
 * Internal representation of circom circuits for the JS implementation of [SnarkJSService].
 * This class is used to manage circuits at runtime.
 *
 * The existence of the compiled .wasm, .zkey and .r1cs files as well as the .ptau file are required at initialization.
 * If one of these files is missing a [FileNotFoundException] will be thrown.
 *
 * The circuit expects these files in a folder named just like the circuit at the given `basePath`.
 *
 * @param name      the name of the circuit; must be the file name of the .circom file without suffix
 * @param basePath  path of the directory that holds the .ptau and circuit files
 * @param ceremony  name of the .ptau file that should be used together with this circuit
 *
 * @property circuitPath  path to circuit-specific files, excluding the .circom file
 * @property zKeyFilePath path to .zKey a file of the circuit
 * @property wasmFilePath path to the .wasm file of the circuit used at proof generation
 * @property r1csFilePath path to .r1cs file of the circuit
 *
 * @property verificationKey  async lazy property that holds the [VerificationKey] of the circuit
 * @property r1csFile         async lazy property that holds the [R1csFile] of the circuit
 * @property zKeyIsValid      async job to verify the zKey if the circuit
 */
@OptIn(DelicateCoroutinesApi::class)
internal class Circuit(
  val name: String,
  private var basePath: String,
  private val ceremony: String,
) {
  private var circuitPath: String
  private var zKeyFilePath: String
  private var wasmFilePath: String
  private var r1csFilePath: String

  private val verificationKey: Deferred<VerificationKey> =
    GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
      val exists =  try { existsSync("$circuitPath/${name}_vk.json") } catch (e: Exception) {
        val fs = js("require('fs')")
        fs.existsSync("$circuitPath/${name}_vk.json") as Boolean
      }
      return@async if (exists) {
        // found exported vKey
        try {
          VerificationKey.fromJson(readFileSync("$circuitPath/${name}_vKey.json", BufferEncoding.utf8))
        } catch (e: Exception) {
          val fs = js("require('fs')")
          VerificationKey.fromJson(fs.readFileSync("$circuitPath/${name}_vKey.json", BufferEncoding.utf8))
        }
      } else {
        // try to export the key and hold it in memory
        try {
          check(zKeyIsValid()) { "zKey for $name is invalid for the current ceremony $ceremony." }
          VerificationKey.fromZKeyFile(zKeyFilePath)
        } catch (e: Exception) {
          throw e
        }
      }
    }

  private val r1csFile: Deferred<R1csFile> =
    GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
      requireFile("$circuitPath/$name.r1cs") { "Couldn't find r1cs-File for $name." }
      return@async R1csFile.fromFile("$circuitPath/$name.r1cs")
    }

  private val zKeyIsValid: Deferred<Boolean> =
    GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
      requireFile(r1csFilePath) { "Couldn't find r1cs-File for $name." }
      requireFile(zKeyFilePath) { "Couldn't find zKey-File for $name." }
      requireFile("$basePath/$ceremony") { "Couldn't find ptau-File $ceremony." }

      return@async snarkjs.zKey.verify(r1csFilePath, "$basePath/$ceremony", zKeyFilePath, null).await()
    }

  init {
    basePath = basePath.removeSuffix("/")
    circuitPath = "$basePath/$name"
    wasmFilePath = "$circuitPath/${name}_js/$name.wasm"
    zKeyFilePath = "$circuitPath/$name.zkey"
    r1csFilePath = "$circuitPath/$name.r1cs"

    requireFile(circuitPath) { "Looks like the circuit $name is not compiled." }
    requireFile(wasmFilePath) { "Couldn't find compiled wasm file for $name." }
    requireFile(r1csFilePath) { "Couldn't find r1cs-File for $name." }
    requireFile(zKeyFilePath) { "Couldn't find zKey-File for $name." }
  }

  /**
   * Blocking function for zKey verification.
   *
   * @return true if zKey is valid, false otherwise
   * @throws [FileNotFoundException] if .r1cs, .zKey or .ptau file couldn't be found.
   */
  suspend fun zKeyIsValid(): Boolean = zKeyIsValid.await()

  /**
   * Returns the number of private inputs for this circuit.
   * The value will be extracted from the .r1cs file.
   *
   * @return number of private inputs
   *
   * @throws [FileNotFoundException] if .r1cs couldn't be found
   */
  suspend fun nPrivate(): Int = r1csFile.await().nPrivate

  /**
   * Returns the number of public inputs for this circuit.
   * The value will be extracted from the .r1cs file.
   *
   * @return number of public inputs
   *
   * @throws [FileNotFoundException] if .r1cs couldn't be found
   */
  suspend fun nPublic(): Int = r1csFile.await().nPublic

  /**
   * Returns the total number of private and public inputs for this circuit.
   * The value will be derived form [nPrivate] and [nPublic].
   *
   * @return total number of inputs
   *
   * @throws [FileNotFoundException] if .r1cs couldn't be found
   */
  suspend fun nInputs(): Int = nPrivate() + nPublic()

  /**
   * Returns the number of outputs for this circuit.
   * The value will be extracted from the .r1cs file.
   *
   * @return number of outputs
   *
   * @throws [FileNotFoundException] if .r1cs couldn't be found
   */
  suspend fun nOutputs(): Int = r1csFile.await().nOutputs

  /**
   * Method to generate a proof with this circuit.
   * The inputs must be provided with the correct names and must be cast to [BigInteger] beforehand.
   *
   * @param inputs inputs for proof generation
   *
   * @return [CircomResult] of the proof generation
   *
   * @throws IllegalArgumentException if the input size doesn't match the number of inputs for the circuits
   */
  @OptIn(ExperimentalSerializationApi::class)
  suspend fun execute(
    inputs: CircuitInputs
  ): CircomResult {
    require(inputs.numberOfInputs == nInputs()) {
      "The number of ${inputs.numberOfInputs} inputs doesn't match the expected number of ${nInputs()} for $name."
    }

    //TODO: catch illegal names

    //println("JSON:")
    //println(moduleJsonBuilder.encodeToString(inputs))

    val result = snarkjs.groth16.fullProve(
      moduleJsonBuilder.encodeToDynamic(inputs),
      wasmFilePath,
      zKeyFilePath,
      null
    ).await().asDynamic()

    val publicSignals = moduleJsonBuilder.decodeFromDynamic<List<BigInteger>>(result["publicSignals"])

    return CircomResult(
      publicSignals.subList(nOutputs(), nOutputs() + nPublic()),
      publicSignals.subList(0, nOutputs()),
      moduleJsonBuilder.decodeFromDynamic(Proof.serializer(), result["proof"])
    )
  }

  /**
   * Method to verify a proof that was generated with this circuit.
   * The public signals must be provided in the same order as they were received from the circuit.
   *
   * No exception will be thrown if the proof wasn't generated with this circuit or the public signals weren't
   * provided in the correct order
   *
   * @param publicSignals list of the public signals that were returned at proof generation
   * @param proof         the proof to be verified
   *
   * @return true if the proof is valid, false otherwise
   */
  @OptIn(ExperimentalSerializationApi::class)
  suspend fun verify(publicSignals: List<BigInteger>, proof: Proof): Boolean {
    return snarkjs.groth16.verify(
      moduleJsonBuilder.encodeToDynamic(verificationKey.await()),
      moduleJsonBuilder.encodeToDynamic(publicSignals),
      moduleJsonBuilder.encodeToDynamic(proof),
      null
    ).await()
  }
}
