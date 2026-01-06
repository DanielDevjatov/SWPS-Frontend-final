package org.fim.wallet.application.service.presentation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.fim.wallet.application.port.`in`.presentation.VerifyPresentationUseCase
import org.fim.wallet.application.port.out.ZKPProvider
import org.fim.wallet.domain.presentation.VerifiablePresentation
import kotlin.reflect.KClass
import kotlin.reflect.createInstance

class VerifyPresentationService(
  private val zkpService: ZKPProvider
) : VerifyPresentationUseCase {
  /**
   * TODO adapt for new Presentation style
   * Verify a given [VerifiablePresentation].
   *
   * See [CreatePresentationService.fullPresentationOf] for additional information on the used circuits.
   *
   * @param presentation      the [VerifiablePresentation] to be verified
   * @param presentationType  reflection class of the given [VerifiablePresentation]
   *
   * @return true if the ZKP of the presentation is valid, false otherwise
   *
   * @see [CreatePresentationService.fullPresentationOf]
   */
  @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
  override suspend fun <T : VerifiablePresentation> verifyPresentation(
    presentation: T,
    presentationType: KClass<out T>
  ): Boolean {

    return zkpService.verify(
      presentationType.createInstance().circuitName(),
      presentation.publicSignals,
      presentation.proof
    )
  }
}
