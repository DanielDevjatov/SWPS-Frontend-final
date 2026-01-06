package org.fim.wallet.domain.dependencyInjection

import kotlinx.serialization.Serializable
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.extensions.encodeToByteArray
import org.fim.wallet.domain.extensions.toByteArrays
import org.kotlincrypto.hash.md.MD5
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Qualifier to be used as key in [CreationExtras] or [DependencyProvider].
 * This is to avoid Strings as keys and enable some compile time checks for this very generic container.
 *
 * The passed reflection objects will be stored as a [MD5] hash internally.
 * Printed as String, this hash is converted to a HexString.
 */
@Serializable
class Qualifier {

  private var hash: ByteArray

  private constructor(hash: ByteArray) {
    this.hash = hash
  }

  /**
   * Constructor for a Qualifier to identify a property inside of [CreationExtras].
   * This ensures that only existing properties are mapped
   * and make it easier to ensure all required values are provided in the [CreationExtras].
   *
   * Example of usage:
   * ```
   *    val qualifier = Qualifier(ConsentCredential::deviceID)
   * ```
   *
   * @param property  [KProperty] reflection of the property that should be used as key.
   *
   * @returns a [Qualifier] for the given property
   */
  constructor(property: KProperty<*>) : this (
    //TODO: using the name may cause conflicts, but hashValue() of property keeps changing every call ...
    hashGenerator.digest(property.name.encodeToByteArray())
  )


  /**
   * Constructor for a Qualifier to identify a service inside of [DependencyProvider],
   * that maps use cases to service instances.
   *
   * Example of usage:
   * ```
   *    val qualifier = Qualifier(IssueCredentialService::class)
   * ```
   *
   * @param classifier  [KClass] reflection of the use case interface to be associated
   *
   * @returns a [Qualifier] for the given [KClass]
   *
   */
  constructor(classifier: KClass<*>) : this(
    hashGenerator.digest(classifier.hashCode().encodeToByteArray())
  )

  /**
   * Constructor for a Qualifier to identify a service inside of [DependencyProvider],
   * that maps use cases and [Credential] types to service instances.
   *
   * Example of usage:
   * ```
   *    val qualifier = Qualifier(GetCredentialUseCase::class, ConsentCredential::class)
   * ```
   *
   * @param classifier  [KClass] reflection of the use case interface to be associated
   * @param subType     optional [KClass] for generic interfaces, null by default
   *
   * @returns a [Qualifier] for the given [KClass]es
   *
   */
  constructor(classifier: KClass<*>, subType: KClass<*>) : this(
    hashGenerator.digest(
      classifier.hashCode().encodeToByteArray()
        + (subType.hashCode().encodeToByteArray())
    )
  )

  /**
   * Constructor for a [Qualifier] to identify a service inside of [DependencyProvider], that maps use cases and
   * [Credential] types to service instances.
   *
   * Example of usage:
   * ```
   * val qualifier = Qualifier(typeOf<GetCredentialUseCase<ConsentCredential>>())
   *
   * //or on semi related types
   * Qualifier(typeOf<IssueCredentialUseCase>(), typeOf<ConsentCredential>())
   * ```
   *
   * @param classifiers one or more [KType] arguments to build the Qualifier from.
   *
   */
  constructor(vararg classifiers: KType) {
    val result = mutableListOf<ByteArray>()
    classifiers.forEach {
      result.addAll(it.toByteArrays())
    }
    this.hash = hashGenerator.digest(result.reduce { acc, it -> acc + it })
  }

  /**
   * Overrides `toString()` in order ro represent the internal hash of a Qualifier as HexString.
   */
  @OptIn(ExperimentalStdlibApi::class)
  override fun toString(): String = hash.toHexString()

  /**
   * Override `equals()` in order to compare Qualifiers only based on the internal hash.
   */
  override fun equals(other: Any?): Boolean = other is Qualifier && hash.contentEquals(other.hash)

  /**
   * Override `hashCode()` in order to compare Qualifiers only based on the internal hash (using `equals()`).
   */
  override fun hashCode(): Int = hash.contentHashCode()

  companion object {
    /**
     * static md5 hash generator
     */
    private val hashGenerator = MD5()
  }
}

/**
 * Auxiliary extension function to create a [Qualifier] from reified generic type, instead of manual usage of typeOf<T>().
 *
 * Example of usage:
 * ```
 * val qualifier = Qualifier<DeleteCredentialService<ConsentCredential>>()
 * ```
 *
 * @param T the type the qualifier should match
 *
 * @return [Qualifier] instance of [T]
 *
 * @receiver [Qualifier]
 */
internal inline fun <reified T> Qualifier() = Qualifier(typeOf<T>())
