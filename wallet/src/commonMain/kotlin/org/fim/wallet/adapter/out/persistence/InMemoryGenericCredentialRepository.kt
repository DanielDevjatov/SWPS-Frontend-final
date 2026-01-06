package org.fim.wallet.adapter.out.persistence

import org.fim.wallet.application.port.out.persistence.GenericCredentialRepository
import org.fim.wallet.domain.credential.Credential
import kotlin.uuid.Uuid

/**
 * In memory implementation of the GenericCredentialRepository that stores the credentials inside a [Map].
 *
 * @see GenericCredentialRepository
 */
class InMemoryGenericCredentialRepository<T: Credential> : GenericCredentialRepository<T> {
  private val credentials = mutableMapOf<Uuid, T>()

  override fun save(credential: T) {
    credentials[credential.id] = credential
  }

  override fun getById(id: Uuid): T? {
    return credentials[id]
  }

  override fun getAll(): Set<T> {
    return credentials.values.toSet()
  }

  override fun delete(id: Uuid) {
    credentials.remove(id)
  }
}
