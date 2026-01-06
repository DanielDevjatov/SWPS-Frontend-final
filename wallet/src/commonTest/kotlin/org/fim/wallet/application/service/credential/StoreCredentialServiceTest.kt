package org.fim.wallet.application.service.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.credential.PrequalificationTypes
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.zkp.CircomBigIntegerUtils.Companion.randomCircomBigInteger
import randomPublicKey
import randomSignature
import kotlin.reflect.createInstance
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class StoreCredentialServiceTest {
  private lateinit var repository: InMemoryGenericCredentialRepository<PrequalificationCredential>
  private lateinit var service: StoreCredentialService<PrequalificationCredential>

  private val credentialExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    PrequalificationCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
    PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
    PrequalificationCredential::gridConnectionArea to "Bayreuth".toPoseidonHashable(),
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val credential = PrequalificationCredential::class.createInstance()
    .create(
      Uuid.random(),
      randomPublicKey(),
      randomPublicKey(),
      randomSignature(),
      Timestamp.now(),
      randomCircomBigInteger(),
      credentialExtras
    ) as PrequalificationCredential

  private val json = credential.toJson()

  @BeforeTest
  fun resetRepository() {
    repository = InMemoryGenericCredentialRepository()
    service = StoreCredentialService(repository)
  }

  
@Test
  fun storeCredential() {
    service.storeCredential(credential)

    assertEquals(credential, repository.getById(credential.id))
  }

  
@Test
  fun storeJson() {
    service.storeCredential(json)
    assertEquals(credential, repository.getById(credential.id))

    //service.storeCredential(PrequalificationCredential::class, json)
    //assertEquals(credential, repository.getById(credential.id))
  }

  
@Test
  fun storeJsonReified() {
    service.storeCredential(json)
    assertEquals(credential, repository.getById(credential.id))
  }

}
