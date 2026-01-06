package org.fim.wallet.application.port.`in`.presentation

import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.presentation.VerifiablePresentation
import kotlin.reflect.KClass

/**
 * Use-case to verify the [Signature] of a given [VerifiablePresentation].
 */
interface VerifyPresentationUseCase {
  /**
   * Verify a given [VerifiablePresentation].
   *
   * @param presentation      the [VerifiablePresentation] to be verified
   * @param presentationType  reflection class of the given [VerifiablePresentation]
   *
   * @return true if the ZKP of the presentation is valid, false otherwise
   */
  suspend fun <T : VerifiablePresentation> verifyPresentation(
    presentation: T,
    presentationType: KClass<out T>
  ): Boolean
}

/**
 * Extension function to verify a [VerifiablePresentation] without explicit declaration of the presentation type.
 *
 * @receiver [VerifyPresentationUseCase]
 *
 * @param presentation  the [VerifiablePresentation] to be verified
 *
 * @return true if the ZKP of the presentation is valid, false otherwise
 *
 * @see VerifyPresentationUseCase.verifyPresentation
 */
suspend inline fun <reified T : VerifiablePresentation>
    VerifyPresentationUseCase.verifyPresentation(presentation: T): Boolean =
  verifyPresentation(presentation, T::class)
