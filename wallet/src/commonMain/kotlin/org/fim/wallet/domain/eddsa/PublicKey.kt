package org.fim.wallet.domain.eddsa

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import kotlinx.serialization.Serializable
import org.fim.wallet.domain.serialize.PublicKeySerializer

/**
 * Domain wrapper class for public eddsa keys.
 * A key is required to consist out of two 32 bytes long [ByteArray]s.
 *
 * The bytes of the key are wrapped to distinguish them from arbitrary byte arrays.
 * Even though having only one property, this class is a data class instead of a value class
 * to be able to override the `equals` method.
 *
 * @property bytes  the public key represented as [List] of [ByteArray]s
 */
@Serializable(PublicKeySerializer::class)
data class PublicKey(val bytes: List<ByteArray>) {
  init {
    require(bytes.size == 2)
  }

  constructor(point: Pair<BigInteger,BigInteger>) : this(listOf(point.first.toByteArray(), point.second.toByteArray()))

  override fun equals(other: Any?): Boolean {
    return other is PublicKey &&
        bytes.first().contentEquals(other.bytes.first()) &&
        bytes.last().contentEquals(other.bytes.last())
  }

  override fun hashCode(): Int {
    return bytes.hashCode()
  }

  override fun toString(): String {
    return this.toBigIntegerList().toString()
  }
}

/**
 * Auxiliary function to cast a [PublicKey] to a [List] of [BigInteger]s
 *
 * @receiver [PublicKey]
 * @return a [List] with [BigInteger] interpretations of the keys inner [ByteArray]s
 */
fun PublicKey.toBigIntegerList(): List<BigInteger> {
  return listOf(
    BigInteger.fromByteArray(this.bytes.first(), Sign.POSITIVE),
    BigInteger.fromByteArray(this.bytes.last(), Sign.POSITIVE)
  )
}

/**
 * Auxiliary function to cast a [List] of [BigInteger]s to a [PublicKey]
 *
 */
fun PublicKey.Companion.fromBigIntegerList(list: List<BigInteger>): PublicKey = PublicKey(Pair(list[0],list[1]))
