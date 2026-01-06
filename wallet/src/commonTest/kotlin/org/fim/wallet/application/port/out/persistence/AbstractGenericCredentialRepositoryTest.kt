package org.fim.wallet.application.port.out.persistence

import org.fim.wallet.domain.credential.Credential
import kotlin.test.*
import kotlin.uuid.Uuid

/**
 * Test implementation for all GenericCredentialRepository implementations.
 * The database will be dropped before each test.
 */
abstract class AbstractGenericCredentialRepositoryTest<T: Credential> {
  private lateinit var credentialRepo: GenericCredentialRepository<T>

  /**
   * Init function for the GenericCredentialRepository to be used.
   *
   * @return instance of a GenericCredentialRepository implementation
   */
  abstract fun createRepository(): GenericCredentialRepository<T>

  /**
   * Init function to generate random test data.
   * Since this test class is generic the data must be provided by the actual test classes.
   *
   * @param id optional parameter in case a Credential with specific id is needed.
   * @return Array of specific Credentials
   */
  abstract fun createCredential(id: Uuid? = null): T

  @BeforeTest
  fun resetDB() {
      credentialRepo = createRepository()
  }

  

@Test
  fun saveCredential() {
    val credential = createCredential()

    credentialRepo.save(credential)

    assertEquals(credentialRepo.getById(credential.id), credential)
  }

  

@Test
  fun updateCredential() {
    val id = Uuid.random()
    val old = createCredential(id)
    val new = createCredential(id)

    assertEquals(old.id, new.id,"Id not equal")

    credentialRepo.save(old)
    credentialRepo.save(new)

    assertEquals(new, credentialRepo.getById(old.id), "Credentials not equal")
    assertEquals(1, credentialRepo.getAll().size,"credential duplicated")
  }

  

@Test
  fun deleteCredentialById() {
    val credential = createCredential()

    credentialRepo.save(credential)
    assertNotNull(credentialRepo.getById(credential.id))

    credentialRepo.delete(credential.id)
    assertNull(credentialRepo.getById(credential.id))
    assertEquals(0, credentialRepo.getAll().size)
  }

  

@Test
  fun getAllCredentials() {
    val credentials = setOf(createCredential(), createCredential(), createCredential())

    credentials.forEach {
      credentialRepo.save(it)
    }

    assertEquals(credentials, credentialRepo.getAll())
  }
}
