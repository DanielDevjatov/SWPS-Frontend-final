package org.fim.wallet.application.service.credential

import org.fim.wallet.application.port.`in`.credential.GetCredentialUseCase
import org.fim.wallet.application.port.out.persistence.GenericCredentialRepository
import org.fim.wallet.domain.credential.Credential
import kotlin.uuid.Uuid

/**
 * Implementation of the [GetCredentialUseCase] using a [GenericCredentialRepository]
 *
 * @property credentialRepository Dependency injection of the credential repository to be used
 *
 * @see GetCredentialUseCase
 * @see GenericCredentialRepository
 */
class GetCredentialService <T: Credential> (
  private val credentialRepository: GenericCredentialRepository<T>
) : GetCredentialUseCase<T> {
  override fun  getCredential(id: Uuid): T {
    return credentialRepository.getById(id)
      ?: throw NoSuchCredentialException("No Credential with ID $id was found.")
  }

  override fun getAllCredentials(): Set<T> {
    return credentialRepository.getAll()
  }
}
