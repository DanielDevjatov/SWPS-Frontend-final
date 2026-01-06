package org.fim.wallet.adapter.out.signature

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.fim.wallet.domain.eddsa.PrivateKey
import kotlin.test.Test
import kotlin.test.assertTrue

class SignatureTest {

  @OptIn(ExperimentalStdlibApi::class)
  private val privateKeys = arrayOf(
    PrivateKey("0001020304050607080900010203040506070809000102030405060708090001".hexToByteArray()),
    PrivateKey("0001020304050607080900010203040506070809000102030405060708090002".hexToByteArray())
  )

  @OptIn(DelicateCoroutinesApi::class)
  private val defaultService: Deferred<EDDSASignatureService> =
    GlobalScope.async { EDDSASignatureService() }

  @Test
  fun keyPair() = runTest {
    val eddsaSignatureService = defaultService.await()
    val privateKey = PrivateKey("my random key of correct size 32".encodeToByteArray())
    val publicKeys = eddsaSignatureService.publicKey(privateKey)
    assertTrue(publicKeys.bytes.isNotEmpty())
  }

  @Test
  fun verify() = runTest {
    val eddsaSignatureService = defaultService.await()
    val message = BigInteger(43)
    val publicKey = eddsaSignatureService.publicKey(privateKeys[1])
    val signature = eddsaSignatureService.sign(message, privateKeys[1])

    assertTrue { eddsaSignatureService.verify(message, signature, publicKey) }
  }
}
