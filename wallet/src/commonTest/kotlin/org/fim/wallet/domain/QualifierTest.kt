package org.fim.wallet.domain

import org.fim.wallet.application.service.credential.DeleteCredentialService
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.dependencyInjection.Qualifier
import org.fim.wallet.domain.extensions.toQualifier
import kotlin.reflect.typeOf
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class QualifierTest {

  @Test
  fun propertyQualifier() {
    val q = Qualifier(ConsentCredential::id)
    val p = Qualifier(ConsentCredential::id)
    val b = Qualifier(ConsentCredential::signature)

    assertEquals(q, p)
    assertNotEquals(q, b)
  }

  @Ignore // name collisions could not yet been solved
  @Test
  fun propertyQualifierOfDifferentClass() {
    val q = Qualifier(ConsentCredential::id)
    val p = Qualifier(PrequalificationCredential::id)

    assertNotEquals(q, p)
  }

  @Test
  fun kTypeQualifier() {
    val q = Qualifier(typeOf<DeleteCredentialService<ConsentCredential>>())
    val p = Qualifier(typeOf<DeleteCredentialService<ConsentCredential>>())
    val b = Qualifier(typeOf<DeleteCredentialService<PrequalificationCredential>>())

    val special = Qualifier(typeOf<DeleteCredentialService<*>>(), typeOf<ConsentCredential>())

    assertEquals(q, p)
    assertNotEquals(q, b)

    assertEquals(special, q)
    assertNotEquals(special, b)
  }

  @Test
  fun reifiedKTypeQualifier() {
    val q = Qualifier(typeOf<DeleteCredentialService<ConsentCredential>>())
    val p = Qualifier<DeleteCredentialService<ConsentCredential>>()
    assertEquals(q, p)
  }

  @Test
  fun kTypeToQualifier() {
    val q = Qualifier(typeOf<DeleteCredentialService<ConsentCredential>>())
    val p = typeOf<DeleteCredentialService<ConsentCredential>>().toQualifier()

    assertEquals(q, p)
  }
}
