package org.fim.wallet.adapter.out.zkp

import kotlinx.coroutines.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import org.fim.wallet.adapter.out.zkp.R1csFile.Companion.fromFile

/**
 * Data class wrapper for r1cs-files.
 * This class doesn't mirror all properties of r1cs-files and wherefore holds a private static JSON builder that will
 * ignore unknown keys.
 *
 * The class is marked as `internal` since it's only supposed to be used from [Circuit] for input and output checks.
 * Because of this the class is also platform-specific to JS and snarkJS due to the usage of [snarkjs.r1cs.exportJson] in [fromFile].
 *
 * @property nConstraints number of constrains of the associated circuit
 * @property nPrivate     number of private inputs of the associated circuit
 * @property nPublic      number of public inputs of the associated circuit
 * @property nLabels      number of labels of the associated circuit
 * @property nOutputs     number of outputs of the associated circuit
 */
@Serializable
internal data class R1csFile(
  val nConstraints: Int,
  @SerialName("nPrvInputs")
  val nPrivate: Int,
  @SerialName("nPubInputs")
  val nPublic: Int,
  val nLabels: Int,
  val nOutputs: Int
) {
  companion object {
    @OptIn(ExperimentalSerializationApi::class)
    private val jsonBuilder = Json {
      ignoreUnknownKeys = true
      classDiscriminatorMode = ClassDiscriminatorMode.NONE
    }

    /**
     * Auxiliary function to parse a JSON formatted string into a [R1csFile]
     *
     * @param jsonString  the JSON formatted string
     *
     * @return the [R1csFile] represented by the JSON string
     *
     * @throws kotlinx.serialization.SerializationException in case of any decoding-specific error
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [R1csFile]
     */
    fun fromJson(jsonString: String): R1csFile = jsonBuilder.decodeFromString(jsonString)

    /**
     * Auxiliary function to load a r1cs-file.
     *
     * @param filePath  the full path to the r1cs-file to load
     *
     * @return the [R1csFile]
     *
     * @see [snarkjs.r1cs.exportJson]
     */
    suspend fun fromFile(filePath: String): R1csFile {
      val json = snarkjs.r1cs.exportJson(filePath, null).await()
      return fromJson(JSON.stringify(json))
    }
  }
}
