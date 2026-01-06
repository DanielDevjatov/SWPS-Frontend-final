package org.fim.wallet.domain.extensions

import org.fim.wallet.domain.dependencyInjection.Qualifier
import kotlin.reflect.KType

/**
 * Auxiliary function to cast a [KType] into a [ByteArray].
 * This function is used by the [Qualifier] to prepare a hash inputs from given [KType]s.
 *
 * The function first casts the [KType.classifier] and then recursively casts all [KType.arguments].
 *
 * @return  flatted [List] of [ByteArray]s starting with the [ByteArray] of the classifier
 * followed by the [ByteArray]s of its arguments
 *
 * @receiver [KType]
 */
internal fun KType.toByteArrays(): List<ByteArray> {
  val result = mutableListOf(this.classifier.hashCode().encodeToByteArray())
  for (argument in arguments) {
    argument.type?.let { result.addAll(it.toByteArrays()) }
  }
  return result
}

/**
 * Auxiliary function to cast a [KType] into a [Qualifier].
 *
 * @return the instance of [Qualifier] for this
 *
 * @receiver [KType]
 */
internal fun KType.toQualifier(): Qualifier = Qualifier(this)
