package org.fim.wallet.domain.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import org.fim.wallet.domain.zkp.CircuitInputs

/**
 * Custom Serializer for [CircuitInputs] using a string serializer.
 *
 * A special Serializer is needed because the resulting JSON should be a "flat map" of key-value pairs of different kinds.
 * (the BigInteger value pairs as well as for the BigInteger arrays)
 *
 * **Note:**
 * This serializer only supports serialization to JSON but no deserialization.
 * This is because [CircuitInputs] are never received as JSONs but instead are only created,
 * and then serialized to pass them to the circuit.
 */
internal object CircuitInputsSerializer : KSerializer<CircuitInputs> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CombinedMaps")

  override fun deserialize(decoder: Decoder): CircuitInputs {
    TODO("Not yet implemented")
  }

  override fun serialize(encoder: Encoder, value: CircuitInputs) {
    val jsonObject = buildJsonObject {
      value.inputs?.forEach { (key, bigIntegerValue) ->
        put(key, Json.encodeToJsonElement(BigIntegerSerializer, bigIntegerValue))
      }
      value.arrayInputs?.forEach { (key, listValue) ->
        put(key, Json.encodeToJsonElement(ListSerializer(BigIntegerSerializer), listValue))
      }
    }
    (encoder as JsonEncoder).encodeJsonElement(jsonObject)
  }
}
