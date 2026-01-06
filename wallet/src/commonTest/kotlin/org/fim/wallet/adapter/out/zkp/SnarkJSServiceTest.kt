package org.fim.wallet.adapter.out.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.coroutines.test.runTest
import org.fim.wallet.domain.zkp.CircuitInputs
import kotlin.test.*

class SnarkJSServiceTest {
  private val inputs = CircuitInputs(listOf("a" to BigInteger(2), "b" to BigInteger(4)))

  @Test
  fun createService() = runTest {
    val service = SnarkJSService.getInstance()
    assertNotNull(service)
  }

  @Test
  fun proof() = runTest {
    val service = SnarkJSService.getInstance()
    val result = service.fullProve("test_circuit", inputs)

    assertEquals(listOf(BigInteger(8)), result.outputs)
    assertEquals(listOf(BigInteger(2)), result.publicInputs)
    terminateSnarkJS()
  }

  @Test
  fun verify() = runTest {
    val service = SnarkJSService.getInstance()
    val result = service.fullProve("test_circuit", inputs)

    assertTrue(service.verify("test_circuit", result.publicSignals, result.proof))
    terminateSnarkJS()
  }

  @Test
  fun proofUnknownCircuit() = runTest {
    val service = SnarkJSService.getInstance()
    assertFailsWith<NoSuchElementException> {
      service.fullProve("not_here", inputs)
    }
  }

  @Test
  fun verifyUnknownCircuit() = runTest {
    val service = SnarkJSService.getInstance()
    val result = service.fullProve("test_circuit", inputs)

    assertFailsWith<NoSuchElementException> {
      (service.verify("not_here", result.publicSignals, result.proof))
    }
    terminateSnarkJS()
  }
}
