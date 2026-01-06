package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.moduleJsonBuilder
import org.fim.wallet.domain.poseidon.PoseidonHashable
import kotlin.reflect.KClass
import kotlin.reflect.createInstance
import kotlin.uuid.Uuid

/**
 * Factory class to create and parse [Credential]s.
 *
 * Example of usage:
 * ```
 *    val credential: PrequalificationCredential =
 *      CredentialFactory().create(id, issuer, holder, signature, timestamp, extras, credentialType)
 * ```
 *
 * @see Credential
 */
internal open class CredentialFactory {

  /**
   * Generic factory function for Credentials.
   * This Method uses the [Credential.create] method defined in the Credential interface.
   *
   * Example of usage:
   * ```
   *    val credential: PrequalificationCredential =
   *      CredentialFactory().create(id, issuer, holder, signature, timestamp, extras, credentialType)
   * ```
   *
   * @param id              the credentials unique id
   * @param issuer          the issuers unique id
   * @param holder          the holders unique id
   * @param signature       the signature of the credential for data validation
   * @param timestamp       the timestamp of creation
   * @param extras          CredentialCreationExtras containing key-value pairs of Credential specific properties
   * @param credentialType  Class reflection to determine concrete Credential type at runtime
   *
   * @return a new Credential of the respective subclass
   *
   * @see Credential.create
   */
  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  fun <T: Credential> create(
    id: Uuid,
    issuer: PublicKey,
    holder: PublicKey,
    signature: Signature,
    timestamp: Timestamp,
    root: BigInteger,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>,
    credentialType: KClass<T>
  ): T {
    //TODO: createInstance is a hacky workaround
    @Suppress("UNCHECKED_CAST")
    return credentialType.createInstance().create(id, issuer, holder, signature, timestamp, root, extras) as T
  }

  /**
   * Inline version of [create], that doesn't require an explicit reflection of the Credential class.
   *
   * Example of usage:
   * ```
   *    val credential: PrequalificationCredential =
   *      CredentialFactory().create(id, issuer, holder, signature, timestamp, extras)
   * ```
   *
   * @param id              the credentials unique id
   * @param issuer          the issuers unique id
   * @param holder          the holders unique id
   * @param signature       the signature of the credential for data validation
   * @param timestamp       the timestamp of creation
   * @param extras          CredentialCreationExtras containing key-value pairs of Credential specific properties
   *
   * @return a new Credential of the respective subclass*
   *
   * @see Credential.create
   */
  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  inline fun <reified T: Credential> create(
    id: Uuid,
    issuer: PublicKey,
    holder: PublicKey,
    signature: Signature,
    timestamp: Timestamp,
    root: BigInteger,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>
  ) : T {
    //TODO: createInstance is a hacky workaround
    return T::class.createInstance().create(id, issuer, holder, signature, timestamp, root, extras) as T
  }

  /**
   * Generic function to parse a JSON into a [Credential].
   * The function will throw an exception if the JSON contains keys that don't match the [Credential]s properties.
   *
   * Because generics can only be checked during compilation, this uses [PolymorphicSerializer]s registered in the
   * [moduleJsonBuilder] to determine the credential type.
   *
   * Example of usage:
   * ```
   *    val credential = CredentialFactory().fromJson(jsonElement)
   * ```
   *
   * @param json  [JsonElement] representation of the Credential to parse
   *
   * @return a new instance of the [Credential]
   *
   * @throws SerializationException   if the given JSON element is not a valid JSON input for the Credential implementation
   * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of Credential
   */
  @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
  //TODO: Currently the "deserialisation" of the extras is handled inside the credentials by recreating the extras after construction
  fun fromJson(json: JsonElement): Credential =
    moduleJsonBuilder.decodeFromJsonElement(PolymorphicSerializer(Credential::class), json)

  /**
   * Inline version to parse a JSON into a [Credential], that doesn't require an explicit reflection of the
   * Credential class.
   *
   * Example of usage:
   * ```
   *    val credential: PrequalificationCredential = CredentialFactory().fromJson(jsonElement)
   * ```
   *
   * @param json  [JsonElement] representation of the Credential to parse
   *
   * @return a new instance of the [Credential] implementation T
   *
   * @throws SerializationException   if the given JSON element is not a valid JSON input for the Credential implementation T
   * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of Credential implementation T
   *
   * @see fromJson
   */
  inline fun <reified T : Credential> fromJson(json: JsonElement): T = fromJson(json) as T
}
