package org.fim.wallet.application.port.out.persistence

import org.fim.wallet.domain.credential.Credential
import kotlin.uuid.Uuid

/**
 * Generic Repository covering basic persistence operations for [Credential]s.
 * This Repository is meant to be used for testing during development.
 * In case more specific operations or queries are required, a credential-specific interface should be defined.
 *
 * Example of usage:
 * ```
 *    val repo: GenericCredentialRepository = InMemoryGenericCredentialRepository()
 *    val credential = PrequalificationCredential()
 *
 *    repo.save(credential)
 * ```
 */
interface GenericCredentialRepository<T: Credential> {

  /**
   * Save or update a [Credential] with initialized ID.
   * See actual implementations to see how updates are handled.
   *
   * @param credential the [Credential] to be saved/updated
   */
  fun save(credential: T)

  /**
   * Query for a [Credential] with given ID.
   * This function will return `null` if no matching [Credential] is found.
   * The responsibility for error handling if a [Credential] was supposed to be found lies with in the calling function.
   *
   * @param id the ID of the [Credential]
   *
   * @return the credential if found, `null` otherwise
   */
  fun getById(id: Uuid): T?

  /**
   * Query all [Credential]s (of a given type [T]).
   *
   * @return (unsorted) set of all [Credential]; may be an empty set.
   */
  fun getAll(): Set<T>

  /**
   * Remove a [Credential] with given ID.
   *
   * @param id the ID of the [Credential] to be removed
   */
  fun delete(id: Uuid)
}
