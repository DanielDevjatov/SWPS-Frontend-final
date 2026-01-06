package org.fim.wallet.domain.dependencyInjection

import kotlinx.coroutines.test.runTest
import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.adapter.out.signature.EDDSASignatureService
import org.fim.wallet.application.service.credential.GetCredentialService
import org.fim.wallet.application.service.credential.IssueCredentialService
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import kotlin.reflect.typeOf
import kotlin.test.*

class DependencyProviderTest {

  @Test
  fun initProvider() = runTest {
    val eddsa = EDDSASignatureService()
    val provider = DependencyProvider(
      Qualifier<IssueCredentialService>() to Factory { IssueCredentialService(eddsa) } as Dependency<*>
    )

    assertNotNull(provider[typeOf<IssueCredentialService>()])
  }

  @Test
  fun notAssociatedService() {
    val provider = DependencyProvider()

    assertFailsWith<NotImplementedError> { provider[typeOf<IssueCredentialService>()] }
  }

  @Test
  fun setDependency() {
    val repo = InMemoryGenericCredentialRepository<ConsentCredential>()
    val key = typeOf<GetCredentialService<ConsentCredential>>()

    val provider = DependencyProvider()

    provider[key] = Factory { GetCredentialService(repo) }
    assertNotNull(provider[key])

    provider[key] = Single { repo }
    assertEquals(repo, provider[key])
  }

  @Test
  fun setDependencyWithVarargs() = runTest {
    val eddsa = EDDSASignatureService()

    val service1 = IssueCredentialService(eddsa)
    val service2 = IssueCredentialService(eddsa)

    assertNotEquals(service1, service2)

    val provider = DependencyProvider(
      Qualifier(typeOf<IssueCredentialService>(), typeOf<ConsentCredential>()) to Single { service1 },
      Qualifier(typeOf<IssueCredentialService>(), typeOf<PrequalificationCredential>()) to Single { service2 }
    )

    assertEquals(service1, provider[typeOf<IssueCredentialService>(), typeOf<ConsentCredential>()])
    assertNotEquals(service2, provider[typeOf<IssueCredentialService>(), typeOf<ConsentCredential>()])

    assertEquals(service2, provider[typeOf<IssueCredentialService>(), typeOf<PrequalificationCredential>()])
    assertNotEquals(service1, provider[typeOf<IssueCredentialService>(), typeOf<PrequalificationCredential>()])
  }
}
