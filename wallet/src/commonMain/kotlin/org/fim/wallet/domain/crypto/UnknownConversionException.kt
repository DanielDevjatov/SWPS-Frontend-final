package org.fim.wallet.domain.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger

/**
 * Exception thrown when an object cannot be converted to [BigInteger].
 * This exception is raised during casting operations when an object
 * cannot be properly converted to its [BigInteger] representation.
 *
 * @property msg The error message describing the conversion failure
 * @property cause The underlying cause of the conversion error, if any
 */
class UnknownConversionException(msg: String, cause: Throwable? = null): Exception(msg, cause)