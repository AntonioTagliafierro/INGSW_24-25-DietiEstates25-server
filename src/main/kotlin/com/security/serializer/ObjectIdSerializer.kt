package com.security.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.types.ObjectId

object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.toHexString()) // Converte l'ObjectId in una stringa
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        val value = decoder.decodeString() // Legge il valore come stringa
        return try {
            ObjectId(value) // Converte la stringa in ObjectId
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid ObjectId format: $value")
        }
    }
}
