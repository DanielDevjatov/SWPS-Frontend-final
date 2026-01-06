package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import joinToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.fim.wallet.domain.MissingCreationExtraException
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.zkp.CircomBigIntegerUtils.Companion.randomCircomBigInteger
import randomPublicKey
import randomSignature
import kotlin.reflect.createInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class CredentialFactoryTest {
  private val factory = CredentialFactory()

  private val id = Uuid.random()
  private val issuer = randomPublicKey()
  private val holder = randomPublicKey()
  private val signature = randomSignature()
  private val timestamp = Timestamp.random()
  private val root = BigInteger(1234)

  private val hemIssuer = randomPublicKey()
  private val hemSignature = randomSignature()
  private val flexibilityGive = BigInteger(43)
  private val flexibilityReceive = BigInteger(3)
  private val prequalificationType = PrequalificationTypes.FLEX

  private val tEnd = Timestamp.random(timestamp.value)
  private val deviceID = randomCircomBigInteger()

  private val expectedConsentCredentialExtras = HashableCreationExtras(
    ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
    ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
    ConsentCredential::tStart to timestamp.toPoseidonHashable(),
    ConsentCredential::tEnd to tEnd.toPoseidonHashable(),
    ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
    ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
    ConsentCredential::flexibilityGive to flexibilityGive.toPoseidonHashable(),
    ConsentCredential::flexibilityReceive to flexibilityReceive.toPoseidonHashable(),
    ConsentCredential::consentForUsage to prequalificationType.toPoseidonHashable()
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val expectedConsentCredential = ConsentCredential::class.createInstance()
    .create(id, issuer, holder, signature, timestamp, root, expectedConsentCredentialExtras)

  private val gridConnectionArea = "Bayreuth"

  private val expectedPrequalificationCredentialExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    PrequalificationCredential::deviceID to deviceID.toPoseidonHashable(),
    PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
    PrequalificationCredential::gridConnectionArea to gridConnectionArea.toPoseidonHashable(),
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val expectedPrequalificationCredential = PrequalificationCredential::class.createInstance()
    .create(id, issuer, holder, signature, timestamp, root, expectedPrequalificationCredentialExtras)

  private val deviceName = "test_name"
  private val deviceOEM = "test_oem"
  private val electronicDescription = "test description"

  private val expectedDeviceSpecificationsCredentialExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    DeviceSpecificationsCredential::deviceName to deviceName.toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceID to deviceID.toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceOEM to deviceOEM.toPoseidonHashable(),
    DeviceSpecificationsCredential::electronicDescription to electronicDescription.toPoseidonHashable(),
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val expectedDeviceSpecificationsCredential =
    DeviceSpecificationsCredential::class.createInstance()
      .create(id, issuer, holder, signature, timestamp, root, expectedDeviceSpecificationsCredentialExtras)

  @Test
  fun createConsentCredential() {
    val consentExtras = HashableCreationExtras(
      ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
      ConsentCredential::tStart to timestamp.toPoseidonHashable(),
      ConsentCredential::tEnd to tEnd.toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
      ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
      ConsentCredential::flexibilityGive to flexibilityGive.toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to flexibilityReceive.toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable()
    )

    val credential: ConsentCredential = factory.create(id, issuer, holder, signature, timestamp, root, consentExtras)

    assertEquals(expectedConsentCredential, credential)
  }

  @Test
  fun createConsentCredentialWithUnknownExtra() {
    val consentExtras = HashableCreationExtras(
      ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
      ConsentCredential::tStart to timestamp.toPoseidonHashable(),
      ConsentCredential::tEnd to tEnd.toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
      ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
      ConsentCredential::flexibilityGive to flexibilityGive.toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to flexibilityReceive.toPoseidonHashable(),
      ::id to "random".toPoseidonHashable(),
    )
    val credential: ConsentCredential = factory.create(id, issuer, holder, signature, timestamp, root, consentExtras)

    assertEquals(expectedConsentCredential, credential)
  }

  @Test
  fun createConsentCredentialWithMissingExtra() {
    val consentExtras = HashableCreationExtras(
      ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
      ConsentCredential::tStart to timestamp.toPoseidonHashable(),
      ConsentCredential::tEnd to tEnd.toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
      ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to flexibilityReceive.toPoseidonHashable(),
    )

    assertFailsWith<MissingCreationExtraException> {
      factory.create<ConsentCredential>(id, issuer, holder, signature, timestamp, root, consentExtras)
    }
  }

  @Test
  fun createConsentCredentialWithIntegerTimestamps() {
    val tStart = Timestamp.random()

    val consentExtras = HashableCreationExtras(
      ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
      ConsentCredential::tStart to tStart.toPoseidonHashable(),
      ConsentCredential::tEnd to (tStart + 1000L).toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
      ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
      ConsentCredential::flexibilityGive to flexibilityGive.toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to flexibilityReceive.toPoseidonHashable(),
      ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable()
    )

    val credential: ConsentCredential = factory.create(id, issuer, holder, signature, timestamp, root, consentExtras)

    assertNotNull(credential)
  }

  @Test
  fun createConsentCredentialFromJson() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"ConsentCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.joinToString()}, 
        "timestamp":${timestamp.toJson()},
        "root":"$root",
        "hemIssuer":${hemIssuer.joinToString()},
        "hemSignature":${hemSignature.joinToString()},
        "flexibilityGive":"$flexibilityGive",
        "flexibilityReceive":"$flexibilityReceive",
        "deviceID":"$deviceID",
        "consentForUsage":"FLEX",
        "tStart":${timestamp.toJson()},
        "tEnd":${tEnd.toJson()}    
      }""".trimIndent()
    )

    val credential: ConsentCredential = factory.fromJson(json) as ConsentCredential
    assertEquals(expectedConsentCredential, credential)
  }

  @Test
  fun createConsentCredentialFromJsonWithUnknownKey() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"ConsentCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.joinToString()},
        "deviceID":"$deviceID",
        "consentForUsage":"FLEX",
        "tStart":${timestamp},
        "tEnd":${tEnd},
        "unknown": "random"
      }""".trimIndent()
    )

    assertFailsWith<SerializationException> {
      factory.fromJson<ConsentCredential>(json)
    }
  }

  @Test
  fun createConsentCredentialFromJsonWithMissingKey() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"ConsentCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.joinToString()}, 
        "timestamp":${timestamp.toJson()},
        "root":"$root",
        "hemIssuer":${hemIssuer.joinToString()},
        "hemSignature":${hemSignature.joinToString()},
        "flexibilityGive":"$flexibilityGive",
        "deviceID":"$deviceID",
        "consentForUsage":"FLEX",
        "tStart":${timestamp.toJson()},
        "tEnd":${tEnd.toJson()}    
      }""".trimIndent()
    )

    assertFailsWith(SerializationException::class) {
      factory.fromJson<ConsentCredential>(json)
    }
  }

  @Test
  fun createPrequalificationCredential() {
    val prequalificationExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      PrequalificationCredential::deviceID to deviceID.toPoseidonHashable(),
      PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
      PrequalificationCredential::gridConnectionArea to gridConnectionArea.toPoseidonHashable(),
    )
    val credential: PrequalificationCredential =
      factory.create(id, issuer, holder, signature, timestamp, root, prequalificationExtras)

    assertEquals(expectedPrequalificationCredential, credential)
  }

  @Test
  fun createPrequalificationCredentialWithUnknownExtra() {
    val prequalificationExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      PrequalificationCredential::deviceID to deviceID.toPoseidonHashable(),
      PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
      PrequalificationCredential::gridConnectionArea to gridConnectionArea.toPoseidonHashable(),
      ::id to "random".toPoseidonHashable(),
    )

    val credential: PrequalificationCredential =
      factory.create(id, issuer, holder, signature, timestamp, root, prequalificationExtras)

    assertEquals(expectedPrequalificationCredential, credential)
  }

  @Test
  fun createPrequalificationCredentialWithMissingExtra() {
    val prequalificationExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      PrequalificationCredential::deviceID to deviceID.toPoseidonHashable(),
      PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
    )
    assertFailsWith<MissingCreationExtraException> {
      factory.create<PrequalificationCredential>(id, issuer, holder, signature, timestamp, root, prequalificationExtras)
    }
  }
  
  @Test
  fun createPrequalificationCredentialFromJson() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"PrequalificationCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.joinToString()},
        "timestamp":${timestamp.toJson()},
        "root":"1234",
        "deviceID":"$deviceID",
        "prequalificationType":"FLEX",
        "gridConnectionArea":"$gridConnectionArea"
      }""".trimIndent()
    )

    val credential: PrequalificationCredential = factory.fromJson(json) as PrequalificationCredential
    assertEquals(expectedPrequalificationCredential, credential)
  }

  @Test
  fun createPrequalificationCredentialFromJsonWithUnknownKey() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"PrequalificationCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.joinToString()},
        "deviceID":"$deviceID",
        "prequalificationType":"FLEX",
        "gridConnectionArea":"$gridConnectionArea",
        "unknown": "random"
      }""".trimIndent()
    )

    assertFailsWith<SerializationException> {
      factory.fromJson<PrequalificationCredential>(json)
    }
  }

  @Test
  fun createPrequalificationCredentialFromJsonWithMissingKey() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"PrequalificationCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.toBigIntegerList()},
        "deviceID":"$deviceID"
      }"""
    )

    assertFailsWith<SerializationException> {
      factory.fromJson<PrequalificationCredential>(json)
    }
  }

  @Test
  fun createDeviceSpecificationsCredential() {
    val deviceSpecificationsExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      DeviceSpecificationsCredential::deviceName to deviceName.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceID to deviceID.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceOEM to deviceOEM.toPoseidonHashable(),
      DeviceSpecificationsCredential::electronicDescription to electronicDescription.toPoseidonHashable(),
    )

    val credential: DeviceSpecificationsCredential =
      factory.create(id, issuer, holder, signature, timestamp, root, deviceSpecificationsExtras)

    assertEquals(expectedDeviceSpecificationsCredential, credential)
  }

  @Test
  fun createDeviceSpecificationsCredentialWithUnknownExtra() {
    val deviceSpecificationsExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      DeviceSpecificationsCredential::deviceName to deviceName.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceID to deviceID.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceOEM to deviceOEM.toPoseidonHashable(),
      DeviceSpecificationsCredential::electronicDescription to electronicDescription.toPoseidonHashable(),
      ::id to "random".toPoseidonHashable(),
    )

    val credential: DeviceSpecificationsCredential =
      factory.create(id, issuer, holder, signature, timestamp, root, deviceSpecificationsExtras)

    assertEquals(expectedDeviceSpecificationsCredential, credential)
  }

  @Test
  fun createDeviceSpecificationsCredentialWithMissingExtra() {
    val deviceSpecificationsExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
      DeviceSpecificationsCredential::deviceName to deviceName.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceOEM to deviceOEM.toPoseidonHashable(),
      DeviceSpecificationsCredential::electronicDescription to electronicDescription.toPoseidonHashable(),
    )

    assertFailsWith<MissingCreationExtraException> {
      factory.create<DeviceSpecificationsCredential>(
        id,
        issuer,
        holder,
        signature,
        timestamp,
        root,
        deviceSpecificationsExtras
      )
    }
  }

  @Test
  fun createDeviceSpecificationsCredentialFromJson() {
    val json = Json.parseToJsonElement(
      """
      {
        "type":"DeviceSpecificationsCredential",
        "id":"$id",
        "issuer":${issuer.joinToString()},
        "holder":${holder.joinToString()},
        "signature":${signature.joinToString()},
        "timestamp":${timestamp.toJson()},
        "root":"1234",
        "deviceName":"$deviceName",
        "deviceID":"$deviceID",
        "deviceOEM":"$deviceOEM",
        "electronicDescription":"$electronicDescription"
      }""".trimIndent()
    )

    val credential: DeviceSpecificationsCredential = factory.fromJson(json) as DeviceSpecificationsCredential
    assertEquals(expectedDeviceSpecificationsCredential, credential)
  }

  @Test
  fun createDeviceSpecificationsCredentialFromJsonWithUnknownKey() {
    val json = Json.parseToJsonElement(
      """
      {
          "id":"$id",
          "issuer":${issuer.joinToString()},
          "holder":${holder.joinToString()},
          "signature":${signature.joinToString()},
          "deviceName":"$deviceName",
          "deviceID":"$deviceID",
          "deviceOEM":"$deviceOEM",
          "electronicDescription":"$electronicDescription",
          "unknown": "random"
      }"""
    )

    assertFailsWith(IllegalArgumentException::class) {
      factory.fromJson<DeviceSpecificationsCredential>(json)
    }
  }

  @Test
  fun createDeviceSpecificationsCredentialFromJsonWithMissingKey() {
    val json = Json.parseToJsonElement(
      """
      {
          "id":"$id",
          "issuer":${issuer.joinToString()},
          "holder":${holder.joinToString()},
          "signature":${signature.joinToString()},
          "deviceID":"$deviceID"
      }"""
    )

    assertFailsWith(SerializationException::class) {
      factory.fromJson<DeviceSpecificationsCredential>(json)
    }
  }
}
