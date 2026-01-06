package org.fim.wallet.domain.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.fim.wallet.domain.Timestamp

/**
 * A custom serializer for the [Timestamp] class to handle serialization and deserialization.
 * The [Timestamp] is serialized as its underlying [Long] value.
 */
object TimestampSerializer : KSerializer<Timestamp> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Timestamp", kotlinx.serialization.descriptors.PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        return Timestamp(decoder.decodeLong())
    }
}
