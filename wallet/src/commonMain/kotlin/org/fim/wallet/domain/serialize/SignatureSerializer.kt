package org.fim.wallet.domain.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.fim.wallet.domain.eddsa.Signature

/**
 * Custom Serializer for [Signature] using a [ListSerializer] for the inner [ByteArray] of the key.
 */
object SignatureSerializer : KSerializer<Signature> {
  private val listSerializer = ListSerializer(BigIntegerSerializer)
  override val descriptor: SerialDescriptor = listSerializer.descriptor
  override fun serialize(encoder: Encoder, value: Signature) = listSerializer.serialize(encoder, value.toBigIntegerList())
  override fun deserialize(decoder: Decoder): Signature = Signature(listSerializer.deserialize(decoder))
}
