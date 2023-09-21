package io.smyrgeorge.test

import io.smyrgeorge.test.domain.Customer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

class Main

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val customer = Customer(5, "test", "test", "test")
    val bytes = ProtoBuf.encodeToByteArray(customer)
    val customer1 = ProtoBuf.decodeFromByteArray<Customer>(bytes)

    println("$customer :: $customer1")

    val schema = ProtoBufSchemaGenerator.generateSchemaText(Customer.serializer().descriptor)
    println(schema)
}