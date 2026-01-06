package org.fim.wallet.domain.serialize

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import org.fim.wallet.domain.eddsa.PublicKey

/**
 * Custom Serializer for [PublicKey] using a (nested) [ListSerializer] for the inner [ByteArray]s of the key.
 */
object PublicKeySerializer : KSerializer<PublicKey> {
  @OptIn(InternalSerializationApi::class)
  private val listSerializer = ListSerializer(ListSerializer(Byte::class.serializer()))
  override val descriptor: SerialDescriptor = listSerializer.descriptor
  override fun serialize(encoder: Encoder, value: PublicKey) =
    listSerializer.serialize(encoder, value.bytes.map { it.toList() })

  override fun deserialize(decoder: Decoder): PublicKey =
    PublicKey(listSerializer.deserialize(decoder).map { it.toByteArray() })
}
