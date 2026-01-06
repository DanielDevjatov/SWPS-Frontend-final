package org.fim.wallet.application.service.credential

import org.fim.wallet.application.port.`in`.credential.DeleteCredentialUseCase
import org.fim.wallet.application.port.out.persistence.GenericCredentialRepository
import org.fim.wallet.domain.credential.Credential
import kotlin.uuid.Uuid

/**
 * Implementation of the [DeleteCredentialUseCase] for [Credential] implementation [T] using a [GenericCredentialRepository]
 *
 * @property credentialRepository Dependency injection of the credential repository to be used
 * @property T                    the used [Credential] implementation
 *
 * @see DeleteCredentialUseCase
 * @see GenericCredentialRepository
 */
class DeleteCredentialService<T: Credential> (
  private val credentialRepository: GenericCredentialRepository<T>
): DeleteCredentialUseCase<T> {
  override fun deleteCredential(id: Uuid) {
    credentialRepository.delete(id)
  }
}
