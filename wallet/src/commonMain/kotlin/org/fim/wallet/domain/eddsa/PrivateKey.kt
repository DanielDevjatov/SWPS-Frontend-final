package org.fim.wallet.domain.eddsa

import kotlinx.serialization.Serializable

/**
 * Domain wrapper class for private eddsa keys.
 * A key is required to be 32 Bytes long.
 *
 * The bytes of the key are wrapped to distinguish them from arbitrary byte arrays.
 * Even though having only one property, this class is a data class instead of a value class
 * to be able to override the `equals` method.
 *
 * @property bytes  the private key represented as [ByteArray]
 */
@Serializable
data class PrivateKey(val bytes: ByteArray) {
  init {
    require(bytes.size == 32)
  }

  override fun equals(other: Any?): Boolean {
    return other is PrivateKey && bytes.contentEquals(other.bytes)
  }

  override fun hashCode(): Int {
    return bytes.contentHashCode()
  }
}
