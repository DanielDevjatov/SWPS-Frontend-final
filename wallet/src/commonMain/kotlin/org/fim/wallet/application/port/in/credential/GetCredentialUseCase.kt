package org.fim.wallet.application.port.`in`.credential

import org.fim.wallet.application.service.credential.NoSuchCredentialException
import org.fim.wallet.domain.credential.Credential
import kotlin.uuid.Uuid

/**
 * Use-case to get a [Credential] of type [T] with given parameters from the wallet.
 */
interface GetCredentialUseCase <T : Credential> {
  /**
   * Query a [Credential] by id.
   *
   * @param id  the id of the [Credential]
   *
   * @return stored [Credential] with the given ID, if it exists
   *
   * @throws NoSuchCredentialException if no [Credential] can be found for the given ID
   */
  fun getCredential(id: Uuid): T

  /**
   * Get all [Credential] of type T.
   *
   * @return [Set] of all stored Credentials of type T, the set will be empty if no credentials are stored in the wallet.
   */
  fun getAllCredentials(): Set<T>
}
