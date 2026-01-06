package org.fim.wallet.domain.dependencyInjection

import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.extensions.toQualifier
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * This Container is used for dependency injection and maps use cases
 * and [Credential] types to specific service instances in a hash map at runtime.
 * For this [Qualifier]s of the use case's and the [Credential] type's [KType] reflection are used as keys.
 * This is because at the time of writing, the "koin" framework (for dependency injection in kotlin) was not designed to
 * handle services with generics as they are used in this project.
 *
 * Note that even if the value type of the hash map is [Any] it's up to the user to ensure to only bind valid instances
 * of the correct service to avoid runtime errors due to wrong mapped services.
 *
 * @param from  varargs of [Pair]s of [Qualifier]s and [Dependency]s with service instances to init the provider with
 *
 * @see Qualifier
 */
internal class DependencyProvider(vararg from: Pair<Qualifier, Dependency<*>>) {

  private val impl: HashMap<Qualifier, Dependency<*>> = HashMap()

  init {
    from.forEach {
      impl[it.first] = it.second
    }
  }

  /**
   * Generic get operator implementation to receive a stored service instance and cast it to the given type [T].
   * The given key will be cast into a [Qualifier] internally.
   * This is to ensure that the provider is only used to map [KType]s.
   *
   * Example of usage:
   * ```
   *    val service: GetCredentialService = provider[typeOf<GetCredentialUseCase<ConsentCredential>>()]
   * ```
   *
   * @param key   a [KType] used as key for the instance to be returned
   *
   * @return the stored service instance
   *
   * @throws NotImplementedError if no service was stored with the given key before
   */
  operator fun <T> get(key: KType): T {
    @Suppress("UNCHECKED_CAST")
    return impl[key.toQualifier()]?.getInstance() as T?: throw NotImplementedError("No implementation for $key.")
  }

  /**
   * Overloaded method of [get] to receive a stored service instance and cast it to the given type [T]
   * that is linked to more than one [KType].
   * The given classifiers will be cast into a [Qualifier] internally.
   *
   * Example of usage:
   * ```
   *    val service: GetCredentialService = provider[typeOf<GetCredentialUseCase<*>>(), typeOf<ConsentCredential>()]
   * ```
   *
   * @param classifiers   [KType]s used as key for the instance to be returned
   *
   * @return the stored service instance
   *
   * @throws NotImplementedError if no service was stored with the given key before
   */
  operator fun <T> get(vararg classifiers: KType): T {
    @Suppress("UNCHECKED_CAST")
    return impl[Qualifier(*classifiers)]?.getInstance() as T
      ?: throw NotImplementedError("No implementation for $classifiers.")
  }

  /**
   * Gets a dependency implementation based on a classifier and subtype.
   *
   * This operator is essential for JSON deserialization of credentials, as it enables
   * runtime type determination through the use of class references. Since generic type information
   * is only available at compile-time, this method allows the system to resolve the correct
   * credential type during JSON parsing.
   *
   * Example of usage:
   * ```
   * // During JSON deserialization
   * val service: GetCredentialService<ConsentCredential> =
   *     dependencies[GetCredentialUseCase::class, ConsentCredential::class]
   * ```
   *
   * @param classifier  The base class or interface type
   * @param subType     The specific implementation or subtype class
   *
   * @return The implementation instance of type [T]
   * @throws NotImplementedError if no implementation is found for the given classifier and subtype
   */
  operator fun <T> get(classifier: KClass<*>, subType: KClass<*>): T {
    @Suppress("UNCHECKED_CAST")
    return impl[Qualifier(classifier, subType)]?.getInstance() as T
      ?: throw NotImplementedError("No $classifier implementation for $subType.")
  }

  /**
   * Generic set operator to store an arbitrary service instance.
   * The given key will be cast into a [Qualifier] internally.
   * This is to ensure that the provider is only used to map to [KType]s.
   *
   * Example of usage:
   * ```
   *    provider[typeOf<GetCredentialUseCase<ConsentCredential>>()] = GetCredentialService( /* ... */ )
   * ```
   *
   * @param key     a [KType] used as key for the instance to be stored
   * @param value   the [Dependency] that contains the service instance to be stored in the provider
   */
  operator fun set(key: KType, value: Dependency<*>) {
    impl[key.toQualifier()] = value
  }

  /**
   * Overloaded method of [set] to store an arbitrary service instance that is linked to a key out of more than one [KType].
   *
   * Example of usage:
   * ```
   *    provider[typeOf<GetCredentialUseCase<*>>(), typeOf<ConsentCredential>()] = GetCredentialService( /* ... */ )
   * ```
   *
   * @param classifiers   the [KType]s used as key for the instance to be stored
   * @param value         the [Dependency] that contains the service instance to be stored in the provider
   */
  operator fun <T: Dependency<*>> set(vararg classifiers: KType, value: T) {
    impl[Qualifier(*classifiers)] = value
  }
}
