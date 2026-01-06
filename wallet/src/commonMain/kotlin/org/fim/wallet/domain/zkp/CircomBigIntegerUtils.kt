package org.fim.wallet.domain.zkp

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import org.fim.wallet.domain.zkp.CircomBigIntegerUtils.Companion.p
import kotlin.random.Random

/**
 * Auxiliary class for working within Circom's finite field.
 */
class CircomBigIntegerUtils {
  companion object {
    val p = BigInteger.parseString("21888242871839275222246405745257275088548364400416034343698204186575808495617")

    /**
     * Creates a random [BigInteger] within Circom's finite field.
     */
    fun randomCircomBigInteger(): BigInteger {
      return BigInteger.fromByteArray(Random.nextBytes(31), Sign.POSITIVE)
    }

    /**
     * Checks if this [BigInteger] value is within Circom's finite field.
     *
     * @return true if the value is less than the field's prime [p]
     */
    fun BigInteger.checkCircomValue() = this < p

    /**
     * Checks if all [BigInteger] values in the list are within Circom's finite field.
     *
     * @return true if all values are less than the field's prime [p]
     */
    fun List<BigInteger>.checkCircomValues() = all { it < p }
  }
}