package org.fim.wallet.application.service.credential

import kotlinx.serialization.json.JsonElement
import org.fim.wallet.application.port.`in`.credential.StoreCredentialUseCase
import org.fim.wallet.application.port.out.persistence.GenericCredentialRepository
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.CredentialFactory

/**
 * Implementation of the [StoreCredentialService] using a [GenericCredentialRepository].
 *
 * @property credentialRepository Dependency injection of the credential repository to be used
 *
 * @see StoreCredentialUseCase
 * @see GenericCredentialRepository
 */
class StoreCredentialService <T: Credential> (
  private val credentialRepository: GenericCredentialRepository<T>
): StoreCredentialUseCase<T> {

  override fun storeCredential(credential: T) {
    credentialRepository.save(credential)
  }

  override fun storeCredential(jsonElement: JsonElement) {
    @Suppress("UNCHECKED_CAST")
    credentialRepository.save(CredentialFactory().fromJson(jsonElement) as T)
  }
}
