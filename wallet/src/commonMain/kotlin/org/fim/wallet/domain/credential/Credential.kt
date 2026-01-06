package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.fim.wallet.application.service.convertToBigInteger
import org.fim.wallet.application.service.presentation.CreatePresentationService
import org.fim.wallet.domain.*
import org.fim.wallet.domain.crypto.BinaryMerkleTree
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.eddsa.toBigIntegerList
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import kotlin.reflect.KProperty1
import kotlin.uuid.Uuid

/**
 * Generic Credential Interface that must be implemented by every Credential type.
 * It defines meta-data properties, those are shared by all credentials as well as generic serialization
 * and casting methods.
 *
 * For the serialization methods to work, implementations must override the properties, due to the limitation
 * of the kotlinx serialization plugin, which makes it impossible to serialize classes those have properties that are not
 * parameters of the primary constructor.
 *
 * Note that all implementations must be annotated with [Serializable].
 *
 * @property id           the credentials unique id
 * @property issuer       the issuers [PublicKey]
 * @property holder       the holders [PublicKey]
 * @property signature    the [Signature] of the credential for data validation
 * @property timestamp    the timestamp of creation
 * @property root         the root of the credentials [BinaryMerkleTree]
 * @property extras       additional credential-specific properties that should be tagged with [Transient] to avoid
 *                        redundant serialization since they are already included in the credential's properties.
 */
@Serializable
@NoArgConstructor
abstract class Credential {
  abstract val id: Uuid                 //1
  abstract val issuer: PublicKey        //2,3
  abstract val holder: PublicKey        //4,5
  abstract val signature: Signature     //6,7,8
  abstract val timestamp: Timestamp     //9
  abstract val root: BigInteger         //10
  abstract val extras: HashableCreationExtras<PoseidonHashable<out Any>>

  /**
   * Factory method for subclasses to enforce a uniform interface for [Credential] creation.
   *
   * @param id          the credentials unique id
   * @param issuer      the issuers [PublicKey]
   * @param holder      the holders [PublicKey]
   * @param signature   the [Signature] of the credential for data validation
   * @param timestamp   the timestamp of creation
   * @property root     the root of the credentials [BinaryMerkleTree]
   * @param extras      [CreationExtras] containing key-value pairs of Credential specific properties
   *
   * @return a new [Credential] of the respective subclass
   *
   * @throws MissingCreationExtraException if a credential value isn't set in the given extras
   *
   * @see CreationExtras
   */
  internal abstract fun create(
    id: Uuid,
    issuer: PublicKey,
    holder: PublicKey,
    signature: Signature,
    timestamp: Timestamp,
    root: BigInteger,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>
  ): Credential

  /**
   * Generic function to cast a [Credential] into a [JsonElement].
   * This function uses a custom [JsonBuilder] which doesn't add the "type" key to the resulting JSON.
   *
   * Example of usage:
   * ```
   *    val credential = PrequalificationCredential()
   *    val json = credential.toJson()
   * ```
   *
   * @return a [JsonElement] encoding the [Credential]
   *
   * @throws SerializationException if the given credential cannot be serialized to JSON.
   */
  fun toJson() = moduleJsonBuilder.encodeToJsonElement(this)


  /**
   * Auxiliary function to convert the meta-data properties of a credential to a [List] of [BigInteger]s.
   *
   * The meta-data properties are those shared among all credential implementations:
   * id, issuer, holder, signature, timestamp and root
   *
   * @return a [List] of [BigInteger] encoding the meta-data properties of the credential
   */
  internal fun baseToBigIntegerList(): List<BigInteger> = listOf(
      id.toPoseidonHashable().hash(),
      issuer.toPoseidonHashable().hash(),
      holder.toPoseidonHashable().hash(),
      signature.toPoseidonHashable().hash(),
      timestamp.toPoseidonHashable().hash(),
      this.root
    )

  /**
   * Auxiliary function to convert the credential to an ordered list of [BigInteger]s.
   * This encodes all members to their hash value (e.g., specified in [extras]).
   *
   * To encode complex properties into a format suitable for circuit inputs rather than a single hash value,
   * one can use the [toExtendedBigIntegerList] function.
   *
   * @return a [List] of [BigInteger] encoding the credential
   *
   * @see toExtendedBigIntegerList
   */
  internal open fun toBigIntegerList(): List<BigInteger> =
    baseToBigIntegerList() + bodyMembers().map { extras[it].hash() }

  /**
   * Auxiliary function to convert the meta-data properties in an information-preserving manner to a
   * [List] of [BigInteger]s.
   *
   * The meta-data properties are those shared among all credential implementations:
   * id, issuer, holder, signature and timestamp
   *
   * @return a [List] of [BigInteger] encoding the meta-data properties
   *
   * @see toBigIntegerList
   * @see toExtendedBigIntegerList
   * @see convertToBigInteger
   */
  internal fun baseToExtendedBigIntegerList(): List<BigInteger> {
    return listOf(id.convertToBigInteger()) +
        issuer.toBigIntegerList() +
        holder.toBigIntegerList() +
        signature.toBigIntegerList() +
        timestamp.convertToBigInteger()
  }

  /**
   * Auxiliary function to convert the credential to an ordered list of [BigInteger]s, which includes all relevant
   * information for a ZKP.
   *
   * The expected format is:
   * - Result of toBigIntegerList()
   * - Result of baseToExtendedBigIntegerList()
   * - [List] of [BigInteger]s containing [Credential] specific information
   *
   * @return a [List] of [BigInteger] encoding the [Credential]
   *
   * @see toBigIntegerList
   * @see baseToExtendedBigIntegerList
   */
  internal abstract fun toExtendedBigIntegerList(): List<BigInteger>

  /**
   * Creates a [BinaryMerkleTree] from the credential's properties.
   * The tree is constructed from the ordered list of the credentials properties hashes specified in the [extras].
   *
   * The default values for the parameters are taken from the credential instance.
   * But can be overridden to create a [BinaryMerkleTree] of credential values before credential creation.
   *
   * @param id        the credentials unique id
   * @param issuer    the issuers [PublicKey]
   * @param holder    the holders [PublicKey]
   * @param timestamp the timestamp of creation
   * @param extras    [HashableCreationExtras] containing key-value pairs of Credential specific properties
   *
   * @return a new [BinaryMerkleTree] containing the credential's properties as leaves
   */
  internal open fun generateTree(
    id: Uuid = this.id,
    issuer: PublicKey = this.issuer,
    holder: PublicKey = this.holder,
    timestamp: Timestamp = this.timestamp,
    extras: HashableCreationExtras<PoseidonHashable<out Any>> = this.extras
  ): BinaryMerkleTree {
    // get the order of members from the specific credential implementation before creating the credential
    // and remove common members to create the string to sing the credential
    @Suppress("UNCHECKED_CAST")
    val attributes = listOf(
      id.toPoseidonHashable(),
      issuer.toPoseidonHashable(),
      holder.toPoseidonHashable(),
      timestamp.toPoseidonHashable()
    ) + bodyMembers().mapNotNull { extras.getOrNull(it) as PoseidonHashable<out Any>? }

    val tree = BinaryMerkleTree(attributes.toTypedArray())
    return tree
  }

  /**
   * Auxiliary function to deal with limited metadata reflection in JS.
   * This function returns a List of all properties of this [Credential].
   *
   * E.g., this is used in [CreatePresentationService.fullPresentationOf] to map credential properties
   *
   * Example of usage:
   * ```
   *  // At runtime with limited reflection options in JS
   *  // one would have a hard time to identify all properties of credential
   *  fun <T: Credential> example(credential: T) {
   *    // (unsafe) cast from KProperty1<*, *> to KProperty1<T, *>
   *    // otherwise further usage would result in type errors due to the * scope
   *    val members = credential.members() as List<KProperty1<T, *>>
   *  }
   * ```
   *
   * @return [List] of [KProperty1] properties for all properties of this [Credential]
   */
  internal abstract fun members(): List<KProperty1<*, *>>


  /**
   * Auxiliary function to get a list of credential-specific properties.
   * This function filters out the common properties shared by all credentials
   * (id, issuer, holder, signature, timestamp, root and extras).
   *
   * @return [List] of [KProperty1] containing only the credential-specific properties
   *
   * @see members
   */
  internal fun bodyMembers() = members()
    .filter { it.name !in listOf("id", "issuer", "holder", "signature", "timestamp", "root", "extras") }

  override fun equals(other: Any?): Boolean {
    return other is Credential &&
        baseToBigIntegerList() == other.baseToBigIntegerList() &&
        bodyMembers().all { extras[it] == other.extras[it] }
  }

  override fun hashCode(): Int = root.hashCode()
}
