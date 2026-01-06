package org.fim.wallet.adapter.`in`.module

import com.ionspin.kotlin.bignum.integer.BigInteger
import createRandomCredentialTrio
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.fim.wallet.adapter.out.signature.EDDSASignatureService
import org.fim.wallet.application.port.`in`.credential.IssueCredentialUseCase
import org.fim.wallet.application.service.convertToBigInteger
import org.fim.wallet.application.service.credential.IssueCredentialService
import org.fim.wallet.application.service.credential.NoSuchCredentialException
import org.fim.wallet.domain.MutableCreationExtras
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.credential.*
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.poseidon.PoseidonHash
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.presentation.AggregatorPresentation
import randomConsentCredential
import randomDeviceSpecificationCredential
import randomPrequalificationCredential
import randomPublicKey
import randomSignature
import kotlin.test.*

class WalletTest {

  @OptIn(DelicateCoroutinesApi::class)
  private val defaultWallet: Deferred<Wallet> = GlobalScope.async { Wallet() }

  @Test
  fun bindAndGetDependency() = runTest {
    val wallet = defaultWallet.await()
    val credential = randomConsentCredential()

    wallet.single<Credential> { credential }

    assertEquals(credential, wallet.get<Credential>())
    assertIs<ConsentCredential>(wallet.get<Credential>())
  }

  @Test
  fun getUninitializedDependency() = runTest {
    val wallet = defaultWallet.await()
    assertFailsWith<NotImplementedError> { wallet.get<Credential>() }
  }

  @Test
  fun initCustomDependencies() = runTest {
    val wallet = Wallet {
      val eddsa = EDDSASignatureService()
      factory<IssueCredentialUseCase> { IssueCredentialService(eddsa) }
    }

    assertFailsWith<NotImplementedError> { wallet.get<Credential>() }
    assertNotNull(wallet.get<IssueCredentialUseCase>())
    assertIs<IssueCredentialService>(wallet.get<IssueCredentialUseCase>())
  }

  @Test
  fun rebindDependency() = runTest {
    val eddsa = EDDSASignatureService()
    val service = IssueCredentialService(eddsa)
    val wallet = Wallet {
      factory<IssueCredentialUseCase> { IssueCredentialService(eddsa) }
    }

    wallet.factory <IssueCredentialUseCase> { randomConsentCredential() }

    assertNotEquals(service, wallet.get<IssueCredentialUseCase>())
    assertIs<ConsentCredential>(wallet.get<IssueCredentialUseCase>())
  }

  @Test
  fun storeCredentials() = runTest {
    val wallet = defaultWallet.await()
    val consentCredentials = Array(2) { randomConsentCredential() }
    val deviceSpecificationsCredential = randomDeviceSpecificationCredential()
    val prequalificationCredential = randomPrequalificationCredential()

    consentCredentials.forEach { wallet.storeCredential(it) }
    wallet.storeCredential(deviceSpecificationsCredential)
    wallet.storeCredential(prequalificationCredential)

    assertEquals(2, wallet.getAllCredentials<ConsentCredential>().size)
    assertEquals(1, wallet.getAllCredentials<DeviceSpecificationsCredential>().size)
    assertEquals(1, wallet.getAllCredentials<PrequalificationCredential>().size)
  }

  @Test
  fun storeCredentialsJsonElement() = runTest {
    val wallet = defaultWallet.await()
    val deviceSpecificationsCredential = randomDeviceSpecificationCredential().toJson()
    wallet.storeCredential(deviceSpecificationsCredential)

    assertEquals(1, wallet.getAllCredentials<DeviceSpecificationsCredential>().size)
  }

  @Test
  fun getCredential() = runTest {
    val wallet = defaultWallet.await()
    val consentCredential = randomConsentCredential()

    wallet.storeCredential(consentCredential)

    assertEquals(consentCredential, wallet.getCredential(consentCredential.id))
    assertFailsWith<NoSuchCredentialException>{
      wallet.getCredential<DeviceSpecificationsCredential>(consentCredential.id)
    }
  }

  @Test
  fun issueCredential() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()
    val (hemPrivate, hemPublic) = eddsa.keyPair()
    val tstart = Timestamp.random()

    val consentExtras = HashableCreationExtras(
      ConsentCredential::deviceID to BigInteger(11).toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
      ConsentCredential::tStart to Timestamp(0).toPoseidonHashable(),
      ConsentCredential::tEnd to Timestamp(15).toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemPublic.toPoseidonHashable(),
      ConsentCredential::hemSignature to randomSignature().toPoseidonHashable(),
      ConsentCredential::flexibilityGive to BigInteger(43).toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to BigInteger(3).toPoseidonHashable(),
    )
    val consentCredential = wallet.issueCredential<ConsentCredential>(holder, consentExtras,hemPrivate)

    assertEquals(wallet.publicKey, consentCredential.issuer)
  }


  @Test
  fun verifyCredential() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()
    val (hemPrivate, hemPublic) = eddsa.keyPair()
    val tstart = Timestamp.random()

    val consentExtras = HashableCreationExtras(
      ConsentCredential::deviceID to BigInteger(11).toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
      ConsentCredential::tStart to Timestamp(0).toPoseidonHashable(),
      ConsentCredential::tEnd to Timestamp(15).toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemPublic.toPoseidonHashable(),
      ConsentCredential::hemSignature to randomSignature().toPoseidonHashable(),
      ConsentCredential::flexibilityGive to BigInteger(43).toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to BigInteger(3).toPoseidonHashable(),
    )
    val consentCredential = wallet.issueCredential<ConsentCredential>(holder, consentExtras,hemPrivate)

    assertTrue(wallet.verifyCredential(consentCredential))
  }

  @Test
  fun deleteCredential() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()

    val consentCredential = randomConsentCredential()
    val deviceSpecificationsExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      DeviceSpecificationsCredential::deviceName to "deviceName".toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceOEM to "OEM-FLEX-01".toPoseidonHashable(),
      DeviceSpecificationsCredential::electronicDescription to "some info".toPoseidonHashable(),
    )

    val deviceSpecificationsCredential =
      CredentialFactory().create<DeviceSpecificationsCredential>(
        consentCredential.id,
        randomPublicKey(),
        randomPublicKey(),
        randomSignature(),
        Timestamp.now(),
        BigInteger(1234),
        deviceSpecificationsExtras
      )

    wallet.storeCredential(consentCredential)
    wallet.storeCredential(deviceSpecificationsCredential)

    wallet.deleteCredential<ConsentCredential>(consentCredential.id)
    assertEquals(deviceSpecificationsCredential, wallet.getCredential(consentCredential.id))
  }



  @Test
  fun createPresentation() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()


    val credentials = createRandomCredentialTrio()
    val creationExtras = MutableCreationExtras(
      AggregatorPresentation::tStart to BigInteger.ZERO,
      AggregatorPresentation::tEnd to BigInteger.ONE
    )

    val presentation: AggregatorPresentation = wallet.createPresentation(credentials,creationExtras)

  }


  @Test
  fun verifyPresentation() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()


    val credentials = createRandomCredentialTrio()
    val creationExtras = MutableCreationExtras(
      AggregatorPresentation::tStart to BigInteger.ZERO,
      AggregatorPresentation::tEnd to BigInteger.ONE
    )

    val presentation: AggregatorPresentation = wallet.createPresentation(credentials,creationExtras)

    assertTrue(wallet.verifyPresentation(presentation))
  }


  @Test
  fun storePresentation() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()


    val credentials = createRandomCredentialTrio()
    val creationExtras = MutableCreationExtras(
      AggregatorPresentation::tStart to BigInteger.ZERO,
      AggregatorPresentation::tEnd to BigInteger.ONE
    )

    val presentation: AggregatorPresentation = wallet.createPresentation(credentials,creationExtras)

    wallet.storePresentation(presentation)
    assertEquals(wallet.getAllPresentations<AggregatorPresentation>().size,1)
  }


  @Test
  fun getPresentation() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()


    val credentials = createRandomCredentialTrio()
    val creationExtras = MutableCreationExtras(
      AggregatorPresentation::tStart to BigInteger.ZERO,
      AggregatorPresentation::tEnd to BigInteger.ONE
    )

    val presentation: AggregatorPresentation = wallet.createPresentation(credentials,creationExtras)
    val presentationId = PoseidonHash().digest(presentation.id.convertToBigInteger())
    wallet.storePresentation(presentation)

    val presentation2: AggregatorPresentation = wallet.getPresentation(presentationId)!!
    assertEquals(presentation,presentation2)
  }


  @Test
  fun deletePresentation() = runTest {
    val wallet = defaultWallet.await()
    val eddsa = EDDSASignatureService()
    val (_, holder) = eddsa.keyPair()


    val credentials = createRandomCredentialTrio()
    val creationExtras = MutableCreationExtras(
      AggregatorPresentation::tStart to BigInteger.ZERO,
      AggregatorPresentation::tEnd to BigInteger.ONE
    )

    val presentation: AggregatorPresentation = wallet.createPresentation(credentials,creationExtras)
    val presentationId = PoseidonHash().digest(presentation.id.convertToBigInteger())
    wallet.storePresentation(presentation)

    wallet.storePresentation(presentation)
    assertEquals(wallet.getAllPresentations<AggregatorPresentation>().size,1)

    wallet.deletePresentation<AggregatorPresentation>(presentationId)

    assertEquals(wallet.getAllPresentations<AggregatorPresentation>().size,0)

  }
}
