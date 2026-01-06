package org.fim.wallet.application.service.credential

import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.domain.credential.PrequalificationCredential
import randomPrequalificationCredential
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class DeleteCredentialServiceTest {
  private var repository = InMemoryGenericCredentialRepository<PrequalificationCredential>()
  private val service = DeleteCredentialService(repository)

  

@Test
  fun deleteExistingCredential() {
    val credential = randomPrequalificationCredential()
    repository.save(credential)

    service.deleteCredential(credential.id)

    assertNull(repository.getById(credential.id))
  }

  

@Test
  fun deleteNonExistentCredential() {
    //should not fail
    service.deleteCredential(Uuid.random())
  }
}
