package org.fim.wallet.domain

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.json.Json
import org.fim.wallet.domain.zkp.Proof
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ProofTest {

  private val validPiA = listOf(
    "17926870501651433469440932091825906806818950245993887677284065644513892178085",
    "2907092583342172637103583049402466160102912255596364643063170270347345212442",
    "1"
  ).map { BigInteger.parseString(it) }
  private val validPiB = listOf(
    listOf(
      "18077105460897654190154295359047000561743061375941878698528035501888735764215",
      "12765626494186734327412066653477605404984164220128918392663044170043870024587"
    ).map { BigInteger.parseString(it) },
    listOf(
      "5839648152037996212290666373335672646013713080347213965483698609211081056364",
      "19673868956719498649248434167952561728062500024534906156081625186861950151414"
    ).map { BigInteger.parseString(it) },
    listOf("1", "0").map { BigInteger.parseString(it) },
  )
  private val validPiC = listOf(
    "2964757091248786089926352030958780885086793700609693938229902314769076138818",
    "15853079342594510194194834048929832328007001628726687307905299604592328546704",
    "1"
  ).map { BigInteger.parseString(it) }

  private val validJson = """{
      "pi_a": ${validPiA.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "pi_b": ${
    validPiB.joinToString(", ", prefix = "[", postfix = "]") {
      it.joinToString(
        ", ",
        prefix = "[",
        postfix = "]"
      ) { element -> "\"$element\"" }
    }
  },
      "pi_c": ${validPiC.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "protocol": "groth16",
      "curve": "bn128"
    }"""

  @Test
  fun fromJson() {
    val proof = Proof.fromJson(validJson)

    assertEquals(validPiA, proof.piA)
    assertEquals(validPiB, proof.piB)
    assertEquals(validPiC, proof.piC)
    assertEquals("groth16", proof.protocol)
    assertEquals("bn128", proof.curve)
  }

  @Test
  fun toJson() {
    val proof = Proof(
      validPiA,
      validPiB,
      validPiC,
      "groth16",
      "bn128"
    )

    assertEquals(Json.parseToJsonElement(validJson), proof.toJson())
  }

  @Test
  fun missingJsonKey() {
    val missingKey = """{
      "pi_a": ${validPiA.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "pi_b": ${
      validPiB.joinToString(", ", prefix = "[", postfix = "]") {
        it.joinToString(
          ", ",
          prefix = "[",
          postfix = "]"
        ) { element -> "\"$element\"" }
      }
    },
      "pi_c": ${validPiC.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "protocol": "groth16"
    }"""

    assertFails { Proof.fromJson(missingKey) }
  }

  @Test
  fun toShortInput() {
    val invalidSize = """{
      "pi_a": ["1"],
      "pi_b": ${
      validPiB.joinToString(", ", prefix = "[", postfix = "]") {
        it.joinToString(
          ", ",
          prefix = "[",
          postfix = "]"
        ) { element -> "\"$element\"" }
      }
    },
      "pi_c": ${validPiC.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "protocol": "groth16",
      "curve": "bn128"
    }"""

    assertFails { Proof.fromJson(invalidSize) }
  }

  @Test
  fun toShortNestedInput() {
    val invalidSize = """{
      "pi_a": ${validPiA.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "pi_b": [["1","1"]["1","1"]["1"]],
      "pi_c": ${validPiC.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }},
      "protocol": "groth16",
      "curve": "bn128"
    }"""

    assertFails { Proof.fromJson(invalidSize) }
  }
}
