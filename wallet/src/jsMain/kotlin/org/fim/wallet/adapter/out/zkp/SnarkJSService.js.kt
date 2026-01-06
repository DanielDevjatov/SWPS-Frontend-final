package org.fim.wallet.adapter.out.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.await
import org.fim.wallet.adapter.out.zkp.SnarkJSService.Companion.getInstance
import org.fim.wallet.application.port.out.ZKPProvider
import org.fim.wallet.domain.zkp.CircomResult
import org.fim.wallet.domain.zkp.CircuitInputs
import org.fim.wallet.domain.zkp.Proof

/**
 * Static logger for [SnarkJSService]
 */
private val LOGGER = KotlinLogging.logger("SnarkJSService")

/**
 * Platform specific implementation of [ZKPProvider] for JS.
 * Due to costly initialization and possible race conditions during file access, this Service implements the singleton
 * pattern for now.
 * Because of this the constructor is internal, and the current instance must be obtained through [getInstance].
 *
 * For now only pre-compiled circuits that are provided in a specific location can be used.
 * Since there is no config injection, yet the folder must be specified using the private static `BASE_DIR` constant.
 * The same holds for the ceremony file and the `PTAU_CEREMONY` constant.
 *
 * **Example of usage:**
 * ```kotlin
 * val service = SnarkJSService.getInstance()
 * ```
 *
 * @property circuits pap of circuit names associated to circuits those were detected on service initialization
 */
actual class SnarkJSService internal constructor(
  private val circuits: Map<String, Circuit>
): ZKPProvider {

  override suspend fun fullProve(
    circuitName: String,
    inputs: CircuitInputs
  ): CircomResult {
    val circuit = circuits[circuitName] ?: throw NoSuchElementException("Couldn't find $circuitName circuit.")
    return circuit.execute(inputs)
  }

  override suspend fun verify(circuitName: String, publicSignals: List<BigInteger>, proof: Proof): Boolean {
    val circuit = circuits[circuitName] ?: throw NoSuchElementException("Couldn't find $circuitName circuit.")
    return circuit.verify(publicSignals, proof)
  }

  actual companion object {
    //TODO: add config
    private const val BASE_DIR = "kotlin/snarkjs_ceremony"
    private const val PTAU_CEREMONY = "pot17_final.ptau"

    //singleton to avoid costly checks on initialization
    private var _instance: SnarkJSService? = null

    /**
     * Auxiliary function to verify the used ptau-file.
     * This function is used in [getInstance].
     *
     * This function is very costly to execute. Because of that it uses a logger to give at least some feedback during
     * execution.
     * For library calls an additional logplease logger instance is used.
     *
     * @param basePath      the path to directory that contains all ceremony and circuit related files; config setting by default
     * @param ptauFileName  the name of the ptau-file; config setting by default
     *
     * @return true if the ceremony is valid, false otherwise
     *
     * @throws FileNotFoundException if the ptau-file couldn't be found
     *
     * @see [snarkjs.powersOfTau.verify]
     * @see [logplease.create]
     */
    suspend fun verifyCeremony(basePath: String = BASE_DIR, ptauFileName: String = PTAU_CEREMONY): Boolean {

      LOGGER.info { "Verifying file tree..." }

      requireFile("$basePath/$ptauFileName") { "Couldn't find ceremony file $ptauFileName." }

      val isValid = try {
        LOGGER.info { "Verifying ceremony file..." }

        //SnarkJS will only work with the logger impl of logplease...
        if (LOGGER.isDebugEnabled()) logplease.setLogLevel("DEBUG")
        else logplease.setLogLevel("INFO")

        snarkjs.powersOfTau.verify(
          "$basePath/$ptauFileName",
          logplease.create("SnarkJSService", null)
        ).await()
      } catch (e: Exception) {
        LOGGER.error(e) { "Error verifying ceremony file: $e" }
        throw e
      }

      LOGGER.info { "Ceremony is valid." }
      return isValid
    }

    /**
     * Returns the service instance or initializes the service at first usage.
     *
     * At initialization the function will verify the specified ceremony file. Since this is a costly operation, it may
     * be skipped by turning off the `safetyMode`. The ceremony won't be verified if the service is already initialized.
     *
     * Also, the function will scan the specified base directory for circom files and ensure whether these circuits were
     * properly compiled. This also takes place only at the first initialization.
     *
     * Only circuits that could be found at service initialization can be used later on.
     *
     * The circuit files must be located as required by the [Circuit] class.
     * The circuit expects these files in a folder named just like the circuit at the given `basePath`.
     *
     * @return the instance of the service
     *
     * @throws IllegalStateException if the ceremony is not valid
     * @throws FileNotFoundException if at least one of the circuits is not compiled properly
     */
    actual fun getInstance(): SnarkJSService {
      return _instance ?: run {

        // TODO: require the .circom file to be placed in the same dir is inconvenient

        val circomFiles = try { node.fs.readdirSync(BASE_DIR) } catch (e: Error) {
          val fs = js("require('fs')")
          fs.readdirSync(BASE_DIR) as Array<String>
        }
          .filter { it.endsWith(".circom") }
          .map { it.removeSuffix(".circom") }


        val circuits: Map<String, Circuit> = circomFiles
          .map { Circuit(it, BASE_DIR, PTAU_CEREMONY) }
          .associateBy { it.name }

        //TODO: return at least an instance with working circuits?
        SnarkJSService(circuits)
      }.also { _instance = it }
    }
  }
}
