package org.fim.wallet.application.port.out.persistence

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.presentation.VerifiablePresentation

/**
 * Generic Repository covering basic persistence operations for [VerifiablePresentation]s.
 * This Repository is meant to be used for testing during development.
 * In case more specific operations or queries are required, a presentation-specific interface should be defined.
 *
 * Example of usage:
 * ```
 *    val repo: GenericPresentationRepository = InMemoryGenericPresentationRepository()
 *    val presentation = PrequalificationPresentation()
 *
 *    repo.save(presentation)
 * ```
 */
interface GenericPresentationRepository<T : VerifiablePresentation> {
  /**
   * Save or update a [VerifiablePresentation] with initialized ID.
   * See actual implementations to see how updates are handled.
   *
   * @param presentation the [VerifiablePresentation] to be saved/updated
   */
  fun save(presentation: T)

  /**
   * Query for a [VerifiablePresentation] with given *poseidon hash of the ID*.
   *
   * This function will return `null` if no matching [VerifiablePresentation] is found.
   * The responsibility for error handling if a [VerifiablePresentation] was supposed to be found
   * lies with in the calling function.
   *
   * The hash instead of the ID itself is used, since the preimage of the ID may not be exposed in the presentation.
   *
   * @param idHash  the hash value for [VerifiablePresentation.id] of the [VerifiablePresentation]
   *
   * @return the [VerifiablePresentation] if found, `null` otherwise
   */
  fun getById(idHash: BigInteger): T?

  /**
   * Query all [VerifiablePresentation] (of a given type [T]).
   *
   * @return (unsorted) set of all [VerifiablePresentation]; may be an empty set.
   */
  fun getAll(): Set<T>

  /**
   * Remove a [VerifiablePresentation] with given **poseidon hash of the ID**.
   *
   * @param idHash  the hash value for [VerifiablePresentation.id] of the [VerifiablePresentation] to be removed
   */
  fun delete(idHash: BigInteger)
}
