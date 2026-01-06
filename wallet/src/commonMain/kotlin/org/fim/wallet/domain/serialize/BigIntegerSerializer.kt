package org.fim.wallet.domain.serialize

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom Serializer for [BigInteger] using a string serializer.
 */
object BigIntegerSerializer: KSerializer<BigInteger> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: BigInteger) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): BigInteger = BigInteger.parseString(decoder.decodeString(), 10)
}
