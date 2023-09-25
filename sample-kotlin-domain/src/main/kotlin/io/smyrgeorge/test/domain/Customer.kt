package io.smyrgeorge.test.domain

import io.smyrgeorge.test.domain.dbz.Source
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoBuf.Default.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoNumber
import io.smyrgeorge.test.domain.dbz.ChangeEvent as DbzChangeEvent

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Customer(
    @ProtoNumber(1)
    val id: Int,
    @ProtoNumber(2)
    val firstName: String,
    @ProtoNumber(3)
    val lastName: String,
    @ProtoNumber(4)
    val email: String
) {
    @Serializable
    data class ChangeEvent(
        override val before: Customer? = null,
        override val after: Customer? = null,
        override val source: Source,
        override val op: String,
        override val tsMs: Long,
    ) : DbzChangeEvent<Customer>, ProtoBufSerializable {

        override fun toProtoBuf(): ByteArray =
            ProtoBuf.encodeToByteArray(serializer(), this)

        companion object {
            fun from(bytes: ByteArray): ChangeEvent =
                decodeFromByteArray(serializer(), bytes)
        }
    }
}
