import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import org.fim.wallet.adapter.out.signature.EDDSASignatureService
import org.fim.wallet.application.port.`in`.credential.issueCredential
import org.fim.wallet.application.service.credential.IssueCredentialService
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.credential.*
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PrivateKey
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.zkp.CircomBigIntegerUtils.Companion.randomCircomBigInteger
import kotlin.random.Random
import kotlin.reflect.createInstance
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun randomSignature(): Signature = Signature(listOf(Random.nextLong().toBigInteger(),Random.nextLong().toBigInteger(),Random.nextLong().toBigInteger()))

fun randomPrivateKey(): PrivateKey = PrivateKey(Random.nextBytes(62))

fun randomPublicKey(): PublicKey = PublicKey(listOf(Random.nextBytes(32), Random.nextBytes(32)))

fun PublicKey.joinToString() =
  this.bytes.joinToString(prefix = "[", separator = ",", postfix = "]") {
    it.joinToString(prefix = "[", separator = ",", postfix = "]")
  }

suspend fun createRandomCredentialTrio(): List<Credential>{
  val deviceID = randomCircomBigInteger()

  val signatureService = EDDSASignatureService()
  val (deviceSpecificationsPrivate, deviceSpecificationsPublic) = signatureService.keyPair()
  val (prequalificationPrivate, prequalificationPublic) = signatureService.keyPair()
  val (ownerPrivate, ownerPublic) = signatureService.keyPair()
  val (hemPrivate, hemPublic) = signatureService.keyPair()
  val (_, aggregatorPublic) = signatureService.keyPair()

  val service = IssueCredentialService(signatureService)

  val deviceSpecificationsExtras: HashableCreationExtras<PoseidonHashable<out Any>> = HashableCreationExtras(
    DeviceSpecificationsCredential::deviceName to "deviceName".toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceID to deviceID.toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceOEM to "deviceOEM".toPoseidonHashable(),
    DeviceSpecificationsCredential::electronicDescription to "electronicDescription".toPoseidonHashable(),
  )

  val deviceSpecificationsCredential: DeviceSpecificationsCredential =
    service.issueCredential(deviceSpecificationsPublic,hemPublic,deviceSpecificationsExtras,deviceSpecificationsPrivate)

  val prequalificationExtras: HashableCreationExtras<PoseidonHashable<out Any>> = HashableCreationExtras(
    PrequalificationCredential::deviceID to deviceID.toPoseidonHashable(),
    PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
    PrequalificationCredential::gridConnectionArea to "Bayreuth".toPoseidonHashable(),
  )

  val prequalificationCredential: PrequalificationCredential =
    service.issueCredential(prequalificationPublic,ownerPublic,prequalificationExtras,prequalificationPrivate)

  val consentExtras = HashableCreationExtras(
    ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
    ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
    ConsentCredential::tStart to Timestamp(0).toPoseidonHashable(),
    ConsentCredential::tEnd to Timestamp(15).toPoseidonHashable(),
    ConsentCredential::hemIssuer to hemPublic.toPoseidonHashable(),
    ConsentCredential::flexibilityGive to BigInteger(43).toPoseidonHashable(),
    ConsentCredential::flexibilityReceive to BigInteger(3).toPoseidonHashable(),
  )

  val consentCredential: ConsentCredential = service.issueCredential(ownerPublic, aggregatorPublic,consentExtras,ownerPrivate,hemPrivate )

  return listOf(deviceSpecificationsCredential, prequalificationCredential, consentCredential)
}

@OptIn(ExperimentalUuidApi::class, ExperimentalJsReflectionCreateInstance::class)
fun randomConsentCredential(): ConsentCredential {
  val timestamp = Timestamp.now()
  val consentExtras =  HashableCreationExtras(
    ConsentCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
    ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
    ConsentCredential::tStart to timestamp.toPoseidonHashable(),
    ConsentCredential::tEnd to timestamp.toPoseidonHashable(),
    ConsentCredential::hemIssuer to randomPublicKey().toPoseidonHashable(),
    ConsentCredential::hemSignature to randomSignature().toPoseidonHashable(),
    ConsentCredential::flexibilityGive to BigInteger(43).toPoseidonHashable(),
    ConsentCredential::flexibilityReceive to BigInteger(3).toPoseidonHashable(),
    ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable()
  )

  return ConsentCredential::class.createInstance()
    .create(
      Uuid.random(),
      randomPublicKey(),
      randomPublicKey(),
      randomSignature(),
      timestamp,
      randomCircomBigInteger(),
      consentExtras
    ) as ConsentCredential
}

@OptIn(ExperimentalUuidApi::class, ExperimentalJsReflectionCreateInstance::class)
fun randomDeviceSpecificationCredential(): DeviceSpecificationsCredential {
  val timestamp = Timestamp.now()
  val deviceSpecificationsExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    DeviceSpecificationsCredential::deviceName to "deviceName".toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceOEM to "OEM-FLEX-01".toPoseidonHashable(),
    DeviceSpecificationsCredential::electronicDescription to "some info".toPoseidonHashable(),
  )

  return DeviceSpecificationsCredential::class.createInstance()
    .create(
      Uuid.random(),
      randomPublicKey(),
      randomPublicKey(),
      randomSignature(),
      timestamp,
      randomCircomBigInteger(),
      deviceSpecificationsExtras
    ) as DeviceSpecificationsCredential
}

@OptIn(ExperimentalUuidApi::class, ExperimentalJsReflectionCreateInstance::class)
fun randomPrequalificationCredential(): PrequalificationCredential {
  val timestamp = Timestamp.now()
  val prequalificationCredentialExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    PrequalificationCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
    PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
    PrequalificationCredential::gridConnectionArea to "Bayreuth".toPoseidonHashable(),
  )
  return PrequalificationCredential::class.createInstance()
    .create(
      Uuid.random(),
      randomPublicKey(),
      randomPublicKey(),
      randomSignature(),
      timestamp,
      randomCircomBigInteger(),
      prequalificationCredentialExtras
    ) as PrequalificationCredential
}
