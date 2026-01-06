package org.fim.wallet.domain.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger


/**
 * Wrapper class interface for objects that can be hashed to a [BigInteger] value.
 *
 * @param T the type of the value to be hashed
 * @param H the type of hash generator that is used to compute the hash
 *
 * @property value the wrapped value that can be represented as a [BigInteger]
 */
interface Hashable<T, H> {
  val value: T

  /**
   * Computes the hash of the stored value without salt.
   *
   * @return the computed hash as [BigInteger]
   */
  fun hash(): BigInteger

  /**
   * Computes the hash of the stored value with the provided salt.
   *
   * @param salt the salt value to be used in hash computation
   * @return the computed hash as [BigInteger]
   */
  fun hash(salt: BigInteger): BigInteger
}
