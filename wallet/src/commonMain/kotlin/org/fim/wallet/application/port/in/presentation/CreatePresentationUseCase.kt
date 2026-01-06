package org.fim.wallet.application.port.`in`.presentation

import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.presentation.VerifiablePresentation
import kotlin.reflect.KClass

/**
 * Use-case to create a new [VerifiablePresentation] with given credentials.
 */
interface CreatePresentationUseCase {
  /**
   * Factory method to create a [VerifiablePresentation] of a given type [T].
   *
   * @param credentials       the [List] of [Credential]s the [VerifiablePresentation] should be created from
   * @param presentationType  reflection class of the requested [VerifiablePresentation] type
   * @param creationExtras    [CreationExtras] of additional properties required for the presentation
   *
   * @return  a [VerifiablePresentation] created from the given credentials and extras.
   */
  suspend fun <T : VerifiablePresentation> fullPresentationOf(
    credentials: List<Credential>,
    presentationType: KClass<T>,
    creationExtras: CreationExtras?
  ): T
}

/**
 * Extension function to create a [VerifiablePresentation] of a given [Credential] without explicit
 * declaration of the presentation type.
 *
 * @receiver [CreatePresentationUseCase]
 *
 * @param credential      the [Credential] the [VerifiablePresentation] should be created from
 * @param creationExtras  [CreationExtras] of additional properties required for the presentation
 *
 * @return  a [VerifiablePresentation] created from the given credentials and extras.
 *
 * @see CreatePresentationUseCase.fullPresentationOf
 */
suspend inline fun <reified T : VerifiablePresentation> CreatePresentationUseCase.fullPresentationOf(
  credential: List<Credential>,
  creationExtras: CreationExtras?
): T = fullPresentationOf(credential, T::class, creationExtras)
