package org.fim.wallet.application.service.presentation

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.application.port.`in`.presentation.PersistencePresentationUseCase
import org.fim.wallet.application.port.out.persistence.GenericPresentationRepository
import org.fim.wallet.domain.presentation.VerifiablePresentation


/**
 * Implementation of the [PersistencePresentationUseCase] to manage persistence operations for [VerifiablePresentation]s
 * of type [T] using a [GenericPresentationRepository] for storage operations.
 *
 * @property repository Dependency injection of the [GenericPresentationRepository] to be used for type [T].
 *
 * @see PersistencePresentationUseCase
 * @see GenericPresentationRepository
 */
class PersistencePresentationService<T : VerifiablePresentation>(
  private val repository: GenericPresentationRepository<T>,
) : PersistencePresentationUseCase<T> {
  override fun deletePresentation(idHash: BigInteger) =
    repository.delete(idHash)

  override fun getPresentation(idHash: BigInteger): T? =
    repository.getById(idHash)

  override fun getAllPresentations(): Set<T> =
    repository.getAll()

  override fun storePresentation(presentation: T) =
    repository.save(presentation)
}
