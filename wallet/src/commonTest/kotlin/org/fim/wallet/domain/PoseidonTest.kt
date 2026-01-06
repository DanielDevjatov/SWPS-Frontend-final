package org.fim.wallet.domain

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import org.fim.wallet.domain.extensions.toArbitraryBigInteger
import org.fim.wallet.domain.poseidon.PoseidonHash
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PoseidonTest {

  private val expectedHashes = mapOf(
    1 to BigInteger.parseString("18586133768512220936620570745912940619677854269274689475585506675881198879027", 10),
    2 to BigInteger.parseString("8645981980787649023086883978738420856660271013038108762834452721572614684349", 10),
    3 to BigInteger.parseString("6018413527099068561047958932369318610297162528491556075919075208700178480084", 10)
  )

  @Test
  fun defaultHash() {
    for (key in expectedHashes.keys) {
      PoseidonHash.digest(key.toBigInteger())
      assertEquals(expectedHashes[key], PoseidonHash.digest(key.toBigInteger()))
    }
  }

  @Test
  fun toManyDefaultInputs() {
    assertFailsWith<IllegalArgumentException> {
      PoseidonHash.digest(*(0..20).toList().map {it.toBigInteger()}.toTypedArray())
    }
  }

  @Test
  fun defaultHashFromString() {
    val string = "test"

    val generator = PoseidonHash()

    assertEquals(generator.digest(string.toArbitraryBigInteger()), PoseidonHash.digest(string))
  }

  @Test
  fun paddedHash() {
    val generator = PoseidonHash(4)
    val inputs = (0..2).map { BigInteger(it) }.toTypedArray()
    val paddedInputs = inputs + BigInteger(inputs.size)

    assertEquals(4, paddedInputs.size)

    /*
    pragma circom 2.1.6;

    include "circomlib/poseidon.circom";
    // include "https://github.com/0xPARC/circom-secp256k1/blob/master/circuits/bigint.circom";

    template Example () {
        signal input a[4];
        signal output out;

        component hash = Poseidon(4);
        hash.inputs[0] <== a[0];
        hash.inputs[1] <== a[1];
        hash.inputs[2] <== a[2];
        hash.inputs[3] <== a[3];

        log("hash", hash.out);
    }

    component main = Example();

    /* INPUT = {
        "a": ["0","1","2","3"]
    } */
     */
    val expectedHash = BigInteger.parseString("4050345352754260300667252706570081029004026400044882557845061748628670512780",10)

    assertEquals(expectedHash, generator.digest(*paddedInputs))
    assertEquals(expectedHash, generator.digest(*inputs))
  }

  @Test
  fun compressedHash() {
    val generator = PoseidonHash(4)
    val inputs = (0..5).map { BigInteger(it) }.toTypedArray()
    val paddedInputs = inputs + Array(9) { BigInteger.ZERO } + BigInteger(inputs.size)

    assertEquals(4 * 4, paddedInputs.size)

    /*
    pragma circom 2.1.6;

    include "circomlib/poseidon.circom";
    // include "https://github.com/0xPARC/circom-secp256k1/blob/master/circuits/bigint.circom";

      template Example () {
          signal input a[16];
          signal output out;

          component leave1 = Poseidon(4);
          leave1.inputs[0] <== a[0];
          leave1.inputs[1] <== a[1];
          leave1.inputs[2] <== a[2];
          leave1.inputs[3] <== a[3];

          component leave2 = Poseidon(4);
          leave2.inputs[0] <== a[4];
          leave2.inputs[1] <== a[5];
          leave2.inputs[2] <== a[6];
          leave2.inputs[3] <== a[7];

          component leave3 = Poseidon(4);
          leave3.inputs[0] <== a[8];
          leave3.inputs[1] <== a[9];
          leave3.inputs[2] <== a[10];
          leave3.inputs[3] <== a[11];

          component leave4 = Poseidon(4);
          leave4.inputs[0] <== a[12];
          leave4.inputs[1] <== a[13];
          leave4.inputs[2] <== a[14];
          leave4.inputs[3] <== a[15];

          component root = Poseidon(4);
          root.inputs[0] <== leave1.out;
          root.inputs[1] <== leave2.out;
          root.inputs[2] <== leave3.out;
          root.inputs[3] <== leave4.out;

          log("hash", root.out);
      }

      component main = Example();

      /* INPUT = {
          "a": ["0","1","2","3","4","5","0","0","0","0","0","0","0","0","0","6"]
      } */
     */
    val expectedHash = BigInteger.parseString("15482001852263410600828590869918405487196228705259318583269655728852579431878",10)

    assertEquals(expectedHash, generator.digest(*paddedInputs))
    assertEquals(expectedHash, generator.digest(*inputs))
  }
}
