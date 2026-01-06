package org.fim.wallet.adapter.out.zkp

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerificationKeyTest {

  @AfterTest
  fun tearDown() = runTest {
    asyncTerminateSnarkJS()
  }

  @Test
  fun loadFromFile() = runTest {
    val vKey = VerificationKey.fromZKeyFile("kotlin/snarkjs_ceremony/test_circuit/test_circuit.zkey")

    js.globals.globalThis.curve_bn128.terminate()

    assertEquals("groth16", vKey.protocol)
    assertEquals("bn128", vKey.curve)
    assertEquals(2, vKey.nPublic)
  }

  @Test
  fun loadFromNonexistentFile() = runTest {
    assertFailsWith<FileNotFoundException> {
      VerificationKey.fromZKeyFile("not_exist.txt")
    }
  }
}
