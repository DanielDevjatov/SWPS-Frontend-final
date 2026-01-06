package org.fim.wallet.domain.eddsa

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.fim.wallet.domain.serialize.SignatureSerializer

/**
 * Domain wrapper class for a (poseidon) eddsa signature.
 *
 * @property r  curve point of the [Signature] encoded as a [Pair] of [BigInteger]
 * @property s  scalar of the [Signature] encoded as a [BigInteger]
 */
@Serializable(SignatureSerializer::class)
data class Signature(val r: Pair<BigInteger, BigInteger>, val s: BigInteger) {

  constructor(values: List<BigInteger>) : this(Pair(values[0], values[1]), values[2])

  fun toBigIntegerList(): List<BigInteger> = listOf(r.first,r.second,s)

  @OptIn(ExperimentalSerializationApi::class)
  fun joinToString(): String {
    val jsonBuilder =  Json { classDiscriminatorMode = ClassDiscriminatorMode.NONE }
    return jsonBuilder.encodeToJsonElement(this).toString()
  }
}
