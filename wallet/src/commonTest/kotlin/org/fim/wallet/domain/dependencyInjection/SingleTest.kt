package org.fim.wallet.domain.dependencyInjection

import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.domain.credential.ConsentCredential
import randomConsentCredential
import kotlin.test.Test
import kotlin.test.assertEquals

class SingleTest {
  @Test
  fun injectSingleton() {
    val d = Single { InMemoryGenericCredentialRepository<ConsentCredential>() }
    val credential = randomConsentCredential()

    val instance1: InMemoryGenericCredentialRepository<ConsentCredential> = d.getInstance()
    instance1.save(credential)

    val instance2 = d.getInstance()

    assertEquals(setOf(credential), instance2.getAll())
    assertEquals(instance1, instance2)
  }
}
