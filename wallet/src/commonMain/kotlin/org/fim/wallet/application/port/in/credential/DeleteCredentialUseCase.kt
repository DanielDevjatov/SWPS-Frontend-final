package org.fim.wallet.application.port.`in`.credential

import org.fim.wallet.domain.credential.Credential
import kotlin.uuid.Uuid

/**
 * Use-case to remove a [Credential] of type [T] with given ID from the wallet.
 */
interface DeleteCredentialUseCase<T: Credential> {
  /**
   * Delete the stored [Credential] with the given ID.
   *
   * @param id  the ID of the credential
   */
  fun deleteCredential(id: Uuid)
}
