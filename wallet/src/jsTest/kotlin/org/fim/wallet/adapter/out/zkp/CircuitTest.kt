package org.fim.wallet.adapter.out.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.coroutines.test.runTest
import org.fim.wallet.domain.zkp.CircuitInputs
import kotlin.test.*


class CircuitTest {

  private fun loadTestCircuit(): Circuit = Circuit(
    "test_circuit",
    "kotlin/snarkjs_ceremony",
    "pot17_final.ptau"
  )

  @AfterTest
  fun tearDown() = runTest {
    asyncTerminateSnarkJS()
  }

  @Test
  fun loadCompiledCircuit() {
    val circuit = loadTestCircuit()
    assertEquals("test_circuit", circuit.name)
  }

  @Test
  fun loadZKey() = runTest {
    val circuit = loadTestCircuit()
    assertTrue(circuit.zKeyIsValid())
  }

  @Test
  fun nonExistingCeremony() = runTest {
    val circuit = Circuit(
      "test_circuit",
      "kotlin/snarkjs_ceremony",
      "not_existing.ptau"
    )

    assertFailsWith<FileNotFoundException> { circuit.zKeyIsValid() }
  }

  @Test
  @Ignore
  fun invalidZKey() = runTest {
    val circuit = Circuit(
      "test_circuit",
      "kotlin/snarkjs_ceremony",
      "pot17_final.ptau"
    )

    assertFalse(circuit.zKeyIsValid())
  }

  @Test
  fun loadR1csFile() = runTest {
    val circuit = loadTestCircuit()

    assertEquals(1, circuit.nPrivate())
    assertEquals(1, circuit.nPublic())
    assertEquals(1, circuit.nOutputs())

    assertEquals(2, circuit.nInputs())
  }

  @Test
  fun executeCircuit() = runTest {
    val circuit = loadTestCircuit()

    val result = circuit.execute(
      CircuitInputs(
        listOf(
        "a" to BigInteger(4),
        "b" to BigInteger(2)
        )
      )
    )

    assertEquals(1, result.outputs.size)
    assertEquals(BigInteger(8), result.outputs.first())

    assertEquals(1, result.publicInputs.size)
    assertEquals(BigInteger(4), result.publicInputs.first())
  }

  @Test
  fun illegalNumberOfInputs() = runTest {
    val circuit = loadTestCircuit()

    assertFailsWith<IllegalArgumentException> {
      circuit.execute(CircuitInputs(listOf("a" to BigInteger(4))))
    }

    assertFailsWith<IllegalArgumentException> {
      circuit.execute(
        CircuitInputs(
          listOf(
          "a" to BigInteger(4),
          "b" to BigInteger(2),
          "c" to BigInteger(1)
          )
        )
      )
    }
  }

  @Test
  @Ignore
  fun illegalInputNames() = runTest {
    //TODO
  }

  @Test
  @Ignore
  fun verifyProof() = runTest {
    val circuit = loadTestCircuit()

    val result = circuit.execute(
      CircuitInputs(
        listOf(
        "a" to BigInteger(4),
        "b" to BigInteger(2)
        )
      )
    )

    assertTrue(circuit.verify(result.publicSignals, result.proof))
  }

  @Test
  @Ignore
  fun invalidProof() = runTest {
    val circuit = loadTestCircuit()

    val result = circuit.execute(
      CircuitInputs(
        listOf(
      "a" to BigInteger(4),
      "b" to BigInteger(2)
        )
      )
    )

    assertFalse(circuit.verify(listOf(BigInteger(183345), BigInteger(3)), result.proof))
  }
}
