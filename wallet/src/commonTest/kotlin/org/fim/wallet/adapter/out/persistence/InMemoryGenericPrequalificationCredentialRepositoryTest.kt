package org.fim.wallet.adapter.out.persistence

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.application.port.out.persistence.AbstractGenericCredentialRepositoryTest
import org.fim.wallet.application.port.out.persistence.GenericCredentialRepository
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
import kotlin.uuid.Uuid

class InMemoryGenericPrequalificationCredentialRepositoryTest : AbstractGenericCredentialRepositoryTest<PrequalificationCredential>(){

  override fun createRepository(): GenericCredentialRepository<PrequalificationCredential> {
    return InMemoryGenericCredentialRepository()
  }

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  override fun createCredential(id: Uuid?): PrequalificationCredential {
    val id = id ?: Uuid.random()
    val prequalificationCredentialExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      PrequalificationCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
      PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
      PrequalificationCredential::gridConnectionArea to "Bayreuth".toPoseidonHashable(),
    )
    return PrequalificationCredential::class.createInstance()
      .create(
        id,
        randomPublicKey(),
        randomPublicKey(),
        randomSignature(),
        Timestamp(1234),
        randomCircomBigInteger(),
        prequalificationCredentialExtras
      ) as PrequalificationCredential
  }
}
