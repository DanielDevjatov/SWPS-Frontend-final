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

class PrequalificationCredentialTest {

  private val id = Uuid.random()
  private val issuer = randomPublicKey()
  private val holder = randomPublicKey()
  private val signature = randomSignature()
  private val timestamp = Timestamp(123456)
  private val root = randomCircomBigInteger()

  val prequalificationCredentialExtras = HashableCreationExtras<PoseidonHashable<out Any>>(
    PrequalificationCredential::deviceID to BigInteger(1234).toPoseidonHashable(),
    PrequalificationCredential::prequalificationType to PrequalificationTypes.FLEX.toPoseidonHashable(),
    PrequalificationCredential::gridConnectionArea to "Bayreuth".toPoseidonHashable(),
  )

  @OptIn(ExperimentalJsReflectionCreateInstance::class)
  private val credential = PrequalificationCredential::class.createInstance()
    .create(id, issuer, holder, signature, timestamp, root, prequalificationCredentialExtras)

  private val json = Json.parseToJsonElement(
    """
    {
      "type":"PrequalificationCredential",
      "id":"$id",
      "issuer":${issuer.joinToString()},
      "holder":${holder.joinToString()},
      "signature":${signature.joinToString()},
      "timestamp":123456,
      "root":"$root",
      "deviceID":"1234",
      "prequalificationType":"FLEX",
      "gridConnectionArea":"Bayreuth"
    }""".trimIndent()
  )

  @Test
  fun jsonFromCredential() {
    assertEquals(json, credential.toJson())
  }
}
