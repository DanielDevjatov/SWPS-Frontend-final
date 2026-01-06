@file:UseSerializers(BigIntegerSerializer::class)

package org.fim.wallet.domain.presentation

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.NoArgConstructor
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.DeviceSpecificationsCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.moduleJsonBuilder
import org.fim.wallet.domain.serialize.BigIntegerSerializer
import org.fim.wallet.domain.zkp.CircuitInputs
import org.fim.wallet.domain.zkp.Proof
import kotlin.uuid.Uuid

/**
 * Generic interface for verifiable presentations that must be implemented by every presentation.
 * It defines meta-data properties, those are shared by all presentations as well as a generic serialization function.
 *
 * For the serialization functions to work, implementations must override the properties, due to the limitation
 * of the serialization plugin, which makes it impossible to serialize classes those have properties that are not
 * parameters of the primary constructor.
 *
 * Note that all implementations must be annotated with @Serializable.
 *
 * @property publicSignals  the public signals of the circom circuit the presentation was created with
 * @property proof          the zkp proof created by the circom circuit
 * @property id             uuid of the presentation
 */
@Serializable
@NoArgConstructor
abstract class VerifiablePresentation{
  abstract val publicSignals: List<BigInteger>
  abstract val proof: Proof
  abstract val id: Uuid

  /**
   * Factory method for subclasses to enforce a uniform interface for [VerifiablePresentation] creation.
   *
   * @param publicSignals     the public signals of the circom circuit the presentation was created with
   * @param proof             the zkp proof created by the circom circuit
   * @param namedCreationExtras   pairs of [Credential] properties mapped to the values to be set for the presentation properties
   *
   * @return a new [VerifiablePresentation] of the respective subclass
   *
   * @throws IllegalArgumentException if a property of the presentation isn't set in the given [namedCreationExtras]
   *
   * @see VerifiablePresentationFactory
   */
  internal abstract fun create(
    publicSignals: List<BigInteger>,
    proof: Proof,
    namedCreationExtras: CreationExtras?
  ): VerifiablePresentation

  /**
   * Generic function to cast a [VerifiablePresentation] into a [JsonElement].
   * This function uses a custom [JsonBuilder] which doesn't add the "type" key to the resulting JSON.
   *
   * @return a [JsonElement] encoding the [VerifiablePresentation]
   *
   * @throws SerializationException if the given credential cannot be serialized to JSON.
   */
  fun toJson() = moduleJsonBuilder.encodeToJsonElement(this)

  /**
   * Creates the [CircuitInputs] for an [VerifiablePresentation].
   *
   * @param credentials [List] of [Credential]s to be included in the presentation. Expects the Credentials to be in triples ([DeviceSpecificationsCredential], [PrequalificationCredential], [ConsentCredential]]
   *
   * @param creationExtras  [CreationExtras] of additional properties required for the presentation
   */
  internal abstract fun createCircuitInputs(credentials: List<Credential>, creationExtras: CreationExtras?): CircuitInputs


  /**
   * Name of the corresponding circom circuit (without file ending).
   */
  internal abstract fun circuitName(): String
}
