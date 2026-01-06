package org.fim.wallet.domain.extensions

import com.ionspin.kotlin.bignum.integer.BigInteger

/**
 * Auxiliary function to convert a [String] to a [BigInteger].
 * The String first will be encoded as a [ByteArray] and then be converted to a positive [BigInteger].
 *
 * **Note:**
 *
 * Only strings with up to 17,179,869,176 chars are supported. (= Int.MAX_VALUE * 8)
 * Do not use this function on Strings that encode a Number with a given base.
 * In this case use [BigInteger.parseString] instead.
 *
 * @returns a positive [BigInteger] from the [ByteArray] representation of this [String]
 *
 * @receiver [String]
 *
 */
fun String.toArbitraryBigInteger(): BigInteger = this.encodeToByteArray().toBigInteger()
