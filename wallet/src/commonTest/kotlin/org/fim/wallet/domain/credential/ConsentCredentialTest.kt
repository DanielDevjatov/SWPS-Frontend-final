package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import joinToString
import kotlinx.serialization.json.Json
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.zkp.CircomBigIntegerUtils.Companion.randomCircomBigInteger
import randomPublicKey
import randomSignature
import kotlin.reflect.createInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

class ConsentCredentialTest {

  private val id = Uuid.random()
  private val issuer = randomPublicKey()
  private val holder = randomPublicKey()
  private val signature = randomSignature()
  private val timestamp = Timestamp(123456)
  private val root = BigInteger(1234)

  private val hemIssuer = randomPublicKey()
  private val hemSignature = randomSignature()

  private val deviceID = randomCircomBigInteger()

  private val consentExtras = HashableCreationExtras(
    ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
    ConsentCredential::consentForUsage to PrequalificationTypes.FLEX.toPoseidonHashable(),
    ConsentCredential::tStart to Timestamp(123).toPoseidonHashable(),
    ConsentCredential::tEnd to Timestamp(456).toPoseidonHashable(),
    ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
    ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
    ConsentCredential::flexibilityGive to BigInteger(43).toPoseidonHashable(),
    ConsentCredential::flexibilityReceive to BigInteger(3).toPoseidonHashable(),
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val credential = ConsentCredential::class.createInstance()
    .create(id, issuer, holder, signature, timestamp, root, consentExtras)

  private val json = Json.parseToJsonElement(
    """
    {
      "type":"ConsentCredential",
      "id":"$id",
      "issuer":${issuer.joinToString()},
      "holder":${holder.joinToString()},
      "signature":${signature.joinToString()},
      "timestamp":123456,
      "root":"1234",
      "deviceID":"$deviceID",
      "hemIssuer":${hemIssuer.joinToString()},
      "hemSignature":${hemSignature.joinToString()},
      "flexibilityGive":"43",
      "flexibilityReceive":"3",
      "consentForUsage":"FLEX",
      "tStart":123,
      "tEnd":456
    }""".trimIndent()
  )

  @Test
  fun jsonFromCredential() {
    assertEquals(json, credential.toJson())
  }

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  @Test
  fun illegalTimeInterval() {
    consentExtras[ConsentCredential::tEnd] = Timestamp(1).toPoseidonHashable()
    assertFailsWith(IllegalArgumentException::class) {
      ConsentCredential::class.createInstance()
        .create(id, issuer, holder, signature, timestamp, root, consentExtras)
    }
  }
}
