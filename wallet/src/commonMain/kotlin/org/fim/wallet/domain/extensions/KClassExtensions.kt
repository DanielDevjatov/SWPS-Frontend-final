package org.fim.wallet.domain.extensions

import org.fim.wallet.domain.dependencyInjection.Qualifier
import kotlin.reflect.KClass

/**
 * Auxiliary extension function to cast [KClass] into [Qualifier].
 *
 * @return [Qualifier] of the [KClass]
 *
 * @receiver [KClass]
 */
internal fun KClass<*>.toQualifier(): Qualifier = Qualifier(this)
