package org.fim.wallet.application.service

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.credential.PrequalificationTypes
import org.fim.wallet.domain.crypto.UnknownConversionException
import org.fim.wallet.domain.poseidon.PoseidonHash
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Function converting a value to a [BigInteger].
 *
 * @return value encoded as a [BigInteger].
 *
 * @throws UnknownConversionException if no conversion is defined for the type of the value
 */
fun Any.convertToBigInteger(): BigInteger {
    throw UnknownConversionException("No conversion specified for this class.")
}

fun BigInteger.convertToBigInteger(): BigInteger = this
fun Timestamp.convertToBigInteger(): BigInteger = BigInteger(this.value)
fun PrequalificationTypes.convertToBigInteger(): BigInteger = PoseidonHash().digest(this.toString())
@OptIn(ExperimentalUuidApi::class)
fun Uuid.convertToBigInteger(): BigInteger = BigInteger.fromByteArray(this.toByteArray(), Sign.POSITIVE)
fun String.convertToBigInteger(): BigInteger = PoseidonHash().digest(this)

