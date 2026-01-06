package org.fim.wallet.application.service.presentation

import org.fim.wallet.application.port.`in`.presentation.CreatePresentationUseCase
import org.fim.wallet.application.port.out.ZKPProvider
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.presentation.AggregatorPresentation
import org.fim.wallet.domain.presentation.VerifiablePresentation
import org.fim.wallet.domain.presentation.VerifiablePresentationFactory
import org.fim.wallet.domain.zkp.CircuitInputs
import kotlin.reflect.KClass
import kotlin.reflect.createInstance

/**
 * Implementation of the [CreatePresentationUseCase] to create [VerifiablePresentation]s
 * using the [VerifiablePresentationFactory] and a [ZKPProvider] for zero-knowledge proof generation.
 *
 * @property zkpService Dependency injection of the [ZKPProvider] to be used for proof generation
 *
 * @see CreatePresentationUseCase
 * @see ZKPProvider
 */
class CreatePresentationService(
    private val zkpService: ZKPProvider
) : CreatePresentationUseCase {

    private val factory = VerifiablePresentationFactory()

    @OptIn(ExperimentalJsReflectionCreateInstance::class)
    override suspend fun <T : VerifiablePresentation> fullPresentationOf(
        credentials: List<Credential>,
        presentationType: KClass<T>,
        creationExtras: CreationExtras?
    ): T {

        val circuitInputs: CircuitInputs =
            presentationType.createInstance().createCircuitInputs(credentials, creationExtras)

        val circomResult = zkpService.fullProve(
            presentationType.createInstance().circuitName(),
            circuitInputs
        )

        return factory.create(circomResult.publicSignals, circomResult.proof, presentationType, null)
    }
}
