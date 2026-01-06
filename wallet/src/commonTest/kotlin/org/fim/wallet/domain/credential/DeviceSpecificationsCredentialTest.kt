package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import joinToString
import kotlinx.serialization.json.Json
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
import kotlin.uuid.Uuid

class DeviceSpecificationsCredentialTest {

  private val id = Uuid.random()
  private val issuer = randomPublicKey()
  private val holder = randomPublicKey()
  private val signature = randomSignature()
  private val timestamp = Timestamp(123456)
  private val root = randomCircomBigInteger()

  private val deviceSpecificationsExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    DeviceSpecificationsCredential::root to root.toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceName to "deviceName".toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
    DeviceSpecificationsCredential::deviceOEM to "OEM-FLEX-01".toPoseidonHashable(),
    DeviceSpecificationsCredential::electronicDescription to "some info".toPoseidonHashable(),
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val credential = DeviceSpecificationsCredential::class.createInstance()
    .create(id, issuer, holder, signature, timestamp, root, deviceSpecificationsExtras)

  private val json = Json.parseToJsonElement(
    """
    {
      "type":"DeviceSpecificationsCredential",
      "id":"$id",
      "issuer":${issuer.joinToString()},
      "holder":${holder.joinToString()},
      "signature":${signature.joinToString()},
      "timestamp":123456,
      "root":"$root",
      "deviceID":"1234",
      "deviceName":"deviceName",
      "deviceOEM":"OEM-FLEX-01",
      "electronicDescription":"some info"
    }""".trimIndent()
  )

  @Test
  fun jsonFromCredential() {
    assertEquals(json, credential.toJson())
  }
}
