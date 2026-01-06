package org.fim.wallet.domain.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.Uuid

/**
 * Custom Serializer for [Uuid] using a string serializer.
 */
object UuidSerializer : KSerializer<Uuid> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Uuid) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): Uuid = Uuid.parse(decoder.decodeString())
}
