package org.fim.wallet.domain.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.MissingCreationExtraException
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.dependencyInjection.Qualifier
import kotlin.reflect.KProperty

/**
 * A type-safe container for credential creation parameters that ensures all values are hashable to [BigInteger].
 *
 * This specialized implementation of [CreationExtras] enforces at compile-time that only values
 * implementing [Hashable] can be stored. This constraint is crucial for credential creation as all
 * credential properties must be convertible to [BigInteger] for cryptographic operations.
 *
 * Example of usage:
 * ```
 * val extras = HashableCreationExtras(
 *     MyCredential::deviceId to deviceId.toPoseidonHashable(),
 *     MyCredential::timestamp to timestamp.toPoseidonHashable()
 * )
 *
 * // This would cause a compile-time error as the value is not hashable by default
 * extras[MyCredential::someField] = SomeCostumeClass()  // Won't compile
 * ```
 *
 * @param H The type bound for stored values, must implement [Hashable]
 *
 * @see CreationExtras
 * @see Hashable
 * @see Credential.create
 */

class HashableCreationExtras<H : Hashable<out Any?, out Any>>(vararg from: Pair<KProperty<*>, H>) : CreationExtras() {

    init {
        from.forEach {
            this[it.first] = it.second
        }
    }

    /**
     * Gets a required hashable value from the creation extras.
     *
     * This operator function is used during credential creation to retrieve mandatory values.
     * If the requested value is not present in the extras, it immediately throws an exception,
     * ensuring that all required values are available during credential creation.
     *
     * Example of usage:
     * ```
     * val deviceId = extras[MyCredential::deviceId] // Throws if deviceId is missing
     * ```
     *
     * @param key The property key for which to retrieve the value
     * @return The hashable value associated with the key
     * @throws MissingCreationExtraException if the requested value is not present in the extras
     */
    @Suppress("UNCHECKED_CAST")
    override operator fun <T> get(key: KProperty<T>): Hashable<T, *> =
        (map[Qualifier(key)] as Hashable<T, *>?)
            ?: throw MissingCreationExtraException("Missing creation extra for $key")

    /**
     * Gets an optional hashable value from the creation extras.
     *
     * This function can be used when the requested value is optional and may be null.
     * Unlike the [get] operator, this function returns null instead of throwing an exception
     * when the requested value is not present.
     *
     * Example of usage:
     * ```
     * val optionalValue = extras.getOrNull(MyCredential::optionalField) // Returns null if not present
     * ```
     *
     * @param key The property key for which to retrieve the value
     * @return The hashable value associated with the key, or null if not present
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrNull(key: KProperty<T>): Hashable<T, *>? = map[Qualifier(key)] as Hashable<T, *>?

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
    operator fun <T> set(key: KProperty<T>, value: H) {
        map[Qualifier(key)] = value
    }

    operator fun <T> set(key: Qualifier, value: Hashable<T, *>) {
        map[key] = value
    }

    /**
     * Creates a deep copy of the HashableCreationExtras instance.
     *
     * This method is crucial when the same creation extras are used for multiple credentials
     * while modifications are made between credential creations. Since maps are passed by reference,
     * any modifications to the original extras would affect all credentials that use them.
     *
     * Example of usage:
     * ```
     *
     * val baseExtras = HashableCreationExtras(/* initial values */)
     *
     * // Create first credential with original extras
     * val credential1 = createCredential(baseExtras.copy())
     *
     * // Change value for second credential
     * // credential1 remains unchanged
     * baseExtras[MyCredential::someValue] = newValue
     *
     * // Create second credential with modified copy
     * val credential2 = createCredential(baseExtras.copy())
     * ```
     *
     * @return A new HashableCreationExtras instance containing copies of all entries
     */
    fun copy(): HashableCreationExtras<H> {
        return HashableCreationExtras<H>().also {
            it.map.putAll(this.map)
        }
    }
}
