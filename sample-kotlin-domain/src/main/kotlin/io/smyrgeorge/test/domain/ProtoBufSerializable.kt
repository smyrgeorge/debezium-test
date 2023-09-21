package io.smyrgeorge.test.domain

sealed interface ProtoBufSerializable {

    fun toProtoBuf(): ByteArray

}