package org.fim.wallet.application.service.credential

import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.domain.credential.PrequalificationCredential
import randomPrequalificationCredential
import kotlin.test.*
import kotlin.uuid.Uuid

class GetCredentialServiceTest {

  private lateinit var repository: InMemoryGenericCredentialRepository<PrequalificationCredential>
  private lateinit var service: GetCredentialService<PrequalificationCredential>

  private val credentials = Array(2) { randomPrequalificationCredential() }

  @BeforeTest
  fun resetRepository() {
    repository = InMemoryGenericCredentialRepository()
    service = GetCredentialService(repository)
  }

  

@Test
  fun getExisting() {
    credentials.forEach {
      repository.save(it)
    }

    assertEquals(credentials[0], service.getCredential(credentials[0].id))
    assertEquals(credentials[1], service.getCredential(credentials[1].id))
  }

  

@Test
  fun tryGetNotExisting() {
    assertFailsWith(NoSuchCredentialException::class) {
      service.getCredential(Uuid.random())
    }
  }

  

@Test
  fun getAll() {
    credentials.forEach {
      repository.save(it)
    }

    assertEquals(credentials.toSet(), service.getAllCredentials())

    repository.delete(credentials[0].id)

    assertEquals(setOf(credentials[1]), service.getAllCredentials())
  }

  

@Test
  fun getAllEmpty() {
    assertEquals(emptySet(), service.getAllCredentials())
  }
}
