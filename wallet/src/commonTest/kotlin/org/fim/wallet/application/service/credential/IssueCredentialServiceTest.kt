package org.fim.wallet.application.service.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.coroutines.test.runTest
import org.fim.wallet.adapter.out.signature.EDDSASignatureService
import org.fim.wallet.application.port.`in`.credential.issueCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.credential.PrequalificationTypes
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import randomPublicKey
import kotlin.test.Test
import kotlin.test.assertEquals

class IssueCredentialServiceTest {

@Test
  fun issuePrequalificationCredential() = runTest {
    val signatureService = EDDSASignatureService()
    val (private, public) = signatureService.keyPair()

    val service = IssueCredentialService(signatureService)

  val extras = HashableCreationExtras<PoseidonHashable<out Any>>()
  extras[PrequalificationCredential::deviceID] = BigInteger(11).toPoseidonHashable()
    extras[PrequalificationCredential::prequalificationType] = PrequalificationTypes.FLEX.toPoseidonHashable()
    extras[PrequalificationCredential::gridConnectionArea] = "Bayreuth".toPoseidonHashable()

    val credential: PrequalificationCredential = service.issueCredential(public, randomPublicKey(), extras, private)

    assertEquals(credential.deviceID, BigInteger(11))
    assertEquals(credential.prequalificationType, PrequalificationTypes.FLEX)
  }
}
