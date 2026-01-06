package org.fim.wallet.domain.presentation

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.zkp.Proof
import kotlin.reflect.KClass
import kotlin.reflect.createInstance

/**
 * Factory class to create and parse [VerifiablePresentation]s.
 *
 * @see VerifiablePresentation
 */
internal open class VerifiablePresentationFactory {
  /**
   * Generic factory function for [VerifiablePresentation]s.
   * This Method uses the [VerifiablePresentation.create] method defined in the [VerifiablePresentation] interface.
   *
   * @param publicSignals     the public signals of the circom circuit the presentation was created with
   * @param proof             the zkp proof created by the circom circuit
   * @param namedProperties   [Credential] properties mapped to the values to be set for the presentation properties
   * @param presentationType  [KClass] reflection to determine concrete [VerifiablePresentation] type at runtime
   *
   * @return a new [VerifiablePresentation] of the respective subclass
   *
   * @see VerifiablePresentation.create
   */
  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  fun <P : VerifiablePresentation> create(
    publicSignals: List<BigInteger>,
    proof: Proof,
    presentationType: KClass<P>,
    namedProperties: CreationExtras?
  ): P {
    @Suppress("UNCHECKED_CAST")
    return presentationType.createInstance().create(publicSignals, proof, namedProperties) as P
  }

  /**
   * Generic factory function using the reified idiom to avoid explicit declaration of the [VerifiablePresentation] type.
   * This Method uses the [VerifiablePresentation.create] method defined in the [VerifiablePresentation] interface.
   *
   * @param publicSignals     the public signals of the circom circuit the presentation was created with
   * @param proof             the zkp proof created by the circom circuit
   * @param namedProperties   pairs of [Credential] properties mapped to the values to be set for the presentation properties
   *
   * @return a new [VerifiablePresentation] of the respective subclass
   *
   * @see VerifiablePresentation.create
   */
  inline fun <reified P : VerifiablePresentation> create(
    publicSignals: List<BigInteger>,
    proof: Proof,
    namedProperties: CreationExtras
  ): P = this.create(publicSignals, proof, P::class, namedProperties)

  /**
   * Generic function to parse a JSON into a [VerifiablePresentation].
   * The function will throw an exception if the JSON contains keys that don't match the [VerifiablePresentation]s properties.
   *
   * Because generics are only checked during compilation and thus can't be used to determine types during runtime,
   * a reflection of the actual class during runtime is required.
   *
   * Note that this is a static function of the interface that isn't inherited by the implementations.
   *
   * @param presentationType  [KClass] reflection of the actual [VerifiablePresentation] type at runtime
   * @param json              [JsonElement] representation of the [VerifiablePresentation] to parse
   *
   * @return a new instance of the [VerifiablePresentation] implementation [T]
   *
   * @throws SerializationException   if the given JSON element is not a valid JSON input for the [VerifiablePresentation] implementation [T]
   * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [VerifiablePresentation] implementation [T]
   */
  @OptIn(ExperimentalSerializationApi::class)
  fun <T : VerifiablePresentation> fromJson(presentationType: KClass<T>, json: JsonElement): T =
    @Suppress("UNCHECKED_CAST")
    Json.decodeFromJsonElement(
      Json.serializersModule.serializer(presentationType, emptyList(), false) as DeserializationStrategy<T>,
      json
    )

  /**
   * Inline version to parse a JSON into a [VerifiablePresentation], that doesn't require an explicit reflection of the
   * [VerifiablePresentation] class.
   *
   * Due to restricted type resolving during runtime, this function may not be used in all contexts.
   *
   * @param json  [JsonElement] representation of the [VerifiablePresentation] to parse
   *
   * @return a new instance of the [VerifiablePresentation] implementation [T]
   *
   * @throws SerializationException   if the given JSON element is not a valid JSON input for the [VerifiablePresentation] implementation [T]
   * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [VerifiablePresentation] implementation [T]
   *
   * @see fromJson
   */
  inline fun <reified T : VerifiablePresentation> fromJson(json: JsonElement): T =
    Json.decodeFromJsonElement(json)
}
