package io.smyrgeorge.test.domain.dbz

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Source(
    @ProtoNumber(1)
    val version: String,
    @ProtoNumber(2)
    val connector: String,
    @ProtoNumber(3)
    val name: String,
    @ProtoNumber(4)
    val db: String,
    @ProtoNumber(5)
    val schema: String,
    @ProtoNumber(6)
    val table: String
)
