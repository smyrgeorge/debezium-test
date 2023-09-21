package io.smyrgeorge.test.domain.dbz

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
interface ChangeEvent<T> {
    @ProtoNumber(1)
    val before: T?

    @ProtoNumber(2)
    val after: T?

    @ProtoNumber(3)
    val source: Source

    @ProtoNumber(4)
    val op: String

    @ProtoNumber(5)
    val tsMs: Long
}