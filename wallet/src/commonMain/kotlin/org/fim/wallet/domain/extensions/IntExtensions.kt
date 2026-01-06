package org.fim.wallet.domain.extensions

import org.fim.wallet.domain.dependencyInjection.Qualifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Auxiliary extension function used by [Qualifier] to cast a [Int] to a 4 Byte long [ByteArray],
 * to pass it to the hash generator.
 *
 * The [KClass] and [KProperty] used to create [Qualifier]s implement the `hashCode()` function that returns an [Int]
 * hash value that needs to be cast into a [ByteArray].
 *
 * Example of usage:
 * ```
 *    val byteArray = classifier.hashCode().toByteArray()
 * ```
 *
 * @return [ByteArray] created form the value
 *
 * @receiver [Int]
 */
internal fun Int.encodeToByteArray(): ByteArray = ByteArray(4) { (this shr (it * 8)).toByte() }
