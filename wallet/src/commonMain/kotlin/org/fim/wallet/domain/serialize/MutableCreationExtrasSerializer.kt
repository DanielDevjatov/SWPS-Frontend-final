package org.fim.wallet.domain.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.dependencyInjection.Qualifier
import org.fim.wallet.domain.poseidon.PoseidonHashable
import kotlin.reflect.KClass
import kotlin.reflect.cast

class MutableHashableCreationExtrasSerializer: KSerializer<HashableCreationExtras<*>> {

    private val listSerializer = ListSerializer(PairSerializer(serializer<Unit>(),PairSerializer(serializer<Unit>(),serializer<Unit>())))
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    private val mapSerializer = MapSerializer(serializer<Qualifier>(), PairSerializer(serializer<KClass<out Any>>(),serializer<String>()))
    override fun serialize(encoder: Encoder, value: HashableCreationExtras<*>){

        val encodedMap = value.map.map { (k,v) ->
            Pair(k,Pair(v!!::class,v.toString()))
         }.toMap()
        mapSerializer.serialize(encoder,encodedMap)
    }
    override fun deserialize(decoder: Decoder): HashableCreationExtras<*> {
        val map:   Map<Qualifier, Pair<KClass<out Any>, String>> = mapSerializer.deserialize(decoder)

        var creationExtras = HashableCreationExtras<PoseidonHashable<out Any>>()

        map.forEach { entry ->
            val entryValue = entry.value.first.cast(entry.value.second) as PoseidonHashable<*>
            creationExtras[entry.key] = entryValue
        }
        return creationExtras
    }


}
