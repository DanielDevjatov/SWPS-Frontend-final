package org.fim.wallet.domain.extensions

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign

/**
 * Auxiliary function to convert a [ByteArray] into a positive [BigInteger].
 *
 * @returns a positive [BigInteger] from this [ByteArray].
 *
 * @receiver [ByteArray]
 */
fun ByteArray.toBigInteger(): BigInteger = BigInteger.fromByteArray(this, Sign.POSITIVE)
