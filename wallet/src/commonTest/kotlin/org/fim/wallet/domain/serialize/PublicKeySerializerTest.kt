package org.fim.wallet.domain.serialize

import joinToString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fim.wallet.domain.eddsa.PublicKey
import randomPublicKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PublicKeySerializerTest {

  private val publicKey = randomPublicKey()
  private val json = """
    ${publicKey.joinToString()}
  """.trimIndent()

  @Test
  fun serialize() {
    assertEquals(publicKey.joinToString(), Json.encodeToString(publicKey))
  }

  @Test
  fun deserialize() {
    val result: PublicKey = Json.decodeFromString(json)
    publicKey.bytes.forEachIndexed { index, it ->
      assertContentEquals(it, result.bytes[index])
    }
    assertEquals(publicKey, Json.decodeFromString(json))
  }
}
