package org.fim.wallet.domain.dependencyInjection

import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.domain.credential.ConsentCredential
import randomConsentCredential
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FactoryTest {
  @Test
  fun injectFactory() {
    val d = Factory { InMemoryGenericCredentialRepository<ConsentCredential>() }
    val credential = randomConsentCredential()

    val instance1: InMemoryGenericCredentialRepository<ConsentCredential> = d.getInstance()
    instance1.save(credential)

    val instance2 = d.getInstance()

    assertEquals(setOf(), instance2.getAll())
    assertNotEquals(instance1, instance2)
  }
}
