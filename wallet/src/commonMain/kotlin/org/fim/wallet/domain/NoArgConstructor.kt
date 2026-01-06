package org.fim.wallet.domain

import org.fim.wallet.domain.credential.Credential

/**
 * Annotation for [Credential] implementations in order to generate a no arg constructor.
 * The compiler plugin and it's config has to be done in gradle.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class NoArgConstructor
