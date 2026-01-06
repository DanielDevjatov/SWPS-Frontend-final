package org.fim.wallet.domain.poseidon

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.credential.PrequalificationTypes
import org.fim.wallet.domain.crypto.Hashable
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.eddsa.toBigIntegerList
import kotlin.uuid.Uuid

/**
 * Implementation of the [Hashable] interface using Poseidon hash function.
 *
 * @param T           The type of value to be hashed
 * @property value    The value to be hashed
 * @property hasher   Hashing operation as a lambda function
 */
class PoseidonHashable<T>(override val value: T, val hasher: (T) -> BigInteger) : Hashable<T, PoseidonHash> {

  override fun hash(): BigInteger = hasher(value)

  override fun hash(salt: BigInteger): BigInteger {
    TODO("Not yet implemented")
  }

  companion object {
    val poseidonHasher = PoseidonHash()
    val poseidonHasher2 = PoseidonHash(2)
    val poseidonHasher3 = PoseidonHash(3)
  }

  override fun equals(other: Any?): Boolean {
    return other is PoseidonHashable<*> && value == other.value
  }

  override fun hashCode(): Int = hash().hashCode()
}

/**
 * Converts [BigInteger] to [PoseidonHashable] instance.
 *
 * @return [PoseidonHashable] instance containing the BigInteger value
 */
fun BigInteger.toPoseidonHashable() =
  PoseidonHashable(this, { value -> PoseidonHashable.poseidonHasher.digest(value) })

/**
 * Converts [String] to [PoseidonHashable] instance.
 *
 * @return [PoseidonHashable] instance containing the String value
 */
fun String.toPoseidonHashable() =
  PoseidonHashable(this, { value -> PoseidonHashable.poseidonHasher.digest(value) })

/**
 * Converts [Uuid] to [PoseidonHashable] instance.
 *
 * @return [PoseidonHashable] instance containing the UUID value
 */
fun Uuid.toPoseidonHashable() =
  PoseidonHashable(
    this,
    { value -> PoseidonHashable.poseidonHasher.digest(BigInteger.fromByteArray(value.toByteArray(), Sign.POSITIVE)) })

/**
 * Converts [Timestamp] to [PoseidonHashable] instance.
 *
 * @return [PoseidonHashable] instance containing the Timestamp value
 */
fun Timestamp.toPoseidonHashable() =
  PoseidonHashable(this, { value -> PoseidonHashable.poseidonHasher.digest(BigInteger(value.value)) })

/**
 * Converts [PublicKey] to [PoseidonHashable] instance using 2-input Poseidon hasher.
 *
 * @return PoseidonHashable instance containing the PublicKey value
 */
fun PublicKey.toPoseidonHashable() =
  PoseidonHashable(this, { value -> PoseidonHashable.poseidonHasher2.digest(*value.toBigIntegerList().toTypedArray()) })

/**
 * Converts [Signature] to [PoseidonHashable] instance using 3-input Poseidon hasher.
 *
 * @return [PoseidonHashable] instance containing the Signature value
 */
fun Signature.toPoseidonHashable() =
  PoseidonHashable(this, { value -> PoseidonHashable.poseidonHasher3.digest(*value.toBigIntegerList().toTypedArray()) })

/**
 * Converts [PrequalificationTypes] to [PoseidonHashable] instance.
 *
 * @return [PoseidonHashable] instance containing the PrequalificationTypes value
 */
fun PrequalificationTypes.toPoseidonHashable() =
  PoseidonHashable(this, { value -> PoseidonHashable.poseidonHasher.digest(value.toString()) })
