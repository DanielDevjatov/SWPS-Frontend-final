package org.fim.wallet.domain

import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.CredentialFactory
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.dependencyInjection.Qualifier
import kotlin.reflect.KProperty

/**
 * Abstract Container for type-specific parameter for the CredentialFactory.
 * Values are stored together with an associated [Qualifier] as key.
 *
 * The Container has no restrictions to the number, name or type of the stored Values.
 * Thus is capable of wrapping arbitrary parameters and helps to archive a uniform create signature
 * for sibling classes with different constructor signatures.
 *
 * @see Credential.create
 * @see CredentialFactory
 */
abstract class CreationExtras {
  internal val map: HashMap<Qualifier, Any?> = HashMap()

  /**
   * Generic implementation of the get operator to receive stored elements as instance of the correct type.
   *
   * The provided [KProperty] will be cast into a [Qualifier] internally.
   * This is to ensure that the container is only used to map (existing) properties, enabling some compile time checks
   * that help the developer to ensure that all required values are mapped to the correct properties.
   *
   * @sample PrequalificationCredential.create
   *
   * @param key the associated [KProperty] of the stored element
   *
   * @return the stored element associated with the given type
   */
  abstract operator fun<T> get(key: KProperty<T>): Any?
}

/**
 * Basic implementation of [CreationExtras] that implements also write access to the stored values.
 *
 * Example of usage:
 * ```
 *    val extras = MutableCreationExtras(
 *      PrequalificationCredential::deviceID to "example_ID",
 *      PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX
 *    )
 *
 *    val credential: PrequalificationCredential = CredentialFactory().create( /*...*/  extras = extras)
 * ```
 *
 * Constructed like this is equivalent to:
 * ```
 *    val extras = MutableCreationExtras(
 *      Pair(PrequalificationCredential::deviceID, "example_ID")
 *      Pair(PrequalificationCredential::prequalificationType, PrequalificationTypes.FLEX)
 *    )
 * ```
 *
 * Or late init with:
 * ```
 *    extras[PrequalificationCredential::deviceID] = "example_ID"
 *    extras[PrequalificationCredential::prequalificationType] = PrequalificationTypes.FLEX
 * ```
 *
 * @param from List of Key-Value Pairs of initial values for the Container
 *
 * @see CreationExtras
 */
class MutableCreationExtras(vararg from: Pair<KProperty<*>, Any>) : CreationExtras() {

  init {
    from.forEach {
      this[it.first] = it.second
    }
  }

  @Suppress("UNCHECKED_CAST")
  override operator fun <T> get(key: KProperty<T>): Any? {
    return map[Qualifier(key)] as T
  }

  /**
   * Generic implementation of the set operator to store elements of arbitrary types.
   *
   * The provided key will be cast into a [Qualifier] internally.
   * This is to ensure that the container is only used to map (existing) properties, enabling some compile time checks
   * that help the developer to ensure that all required values are mapped to the correct properties.
   *
   * Example of usage:
   * ```
   *    val extras = MutableCreationExtras()
   *    extras[PrequalificationCredential::deviceID] = "example_ID"
   *    extras[PrequalificationCredential::prequalificationType] = PrequalificationTypes.FLEX
   * ```
   *
   * @param key   the [KProperty] to associate with the value that is to be stored
   * @param value the value to be stored
   */
  operator fun<T> set(key: KProperty<T>, value: T) {
    map[Qualifier(key)] = value
  }
}
