package org.fim.wallet.application.port.`in`.presentation

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.presentation.VerifiablePresentation

/**
 * Use-case to bundle all repository usages for [VerifiablePresentation]s in the wallet.
 */
interface PersistencePresentationUseCase<T : VerifiablePresentation> {

  /**
   * Delete the stored [VerifiablePresentation] with the given ID.
   *
   * @param idHash  the poseidon hash of the id of the [VerifiablePresentation]
   */
  fun deletePresentation(idHash: BigInteger)

  /**
   * Query a [VerifiablePresentation] by id.
   *
   * @param idHash  the poseidon hash of the id of the [VerifiablePresentation]
   *
   * @return stored [VerifiablePresentation] with the given ID, if it exists
   */
  fun getPresentation(idHash: BigInteger): T?

  /**
   * Get all [VerifiablePresentation] of type [T].
   *
   * @return [Set] of all stored [VerifiablePresentation]s of type [T],
   * the set will be empty if no presentations of this type are stored in the wallet.
   */
  fun getAllPresentations(): Set<T>

  /**
   * Store a given [VerifiablePresentation] inside the wallet.
   *
   * @param presentation the [VerifiablePresentation] instance to be stored
   */
  fun storePresentation(presentation: T)
}
