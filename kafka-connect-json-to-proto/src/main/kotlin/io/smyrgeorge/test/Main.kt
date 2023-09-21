package io.smyrgeorge.test

import com.google.protobuf.DynamicMessage
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider

class Main

fun main() {
    val c = ProtobufConverter()
    println(c)

    val schemaRegistryUrl = "http://localhost:58002/apis/ccompat/v7"
    val schemaProvider = ProtobufSchemaProvider()
    val schemaRegistryClient = CachedSchemaRegistryClient(
        /* baseUrls = */ schemaRegistryUrl,
        /* identityMapCapacity = */ 100,
        /* providers = */ listOf(schemaProvider),
        /* originals = */ emptyMap<String, Any>()
    )

    val topic = "dbserver1.inventory.customers"
    val meta: SchemaMetadata = schemaRegistryClient.getLatestSchemaMetadata(topic)

    val protoSchema = try {
        schemaRegistryClient.getSchemaById(meta.id) as ProtobufSchema
    } catch (e: Exception) {
        throw e
    }

    val builder = DynamicMessage.newBuilder(protoSchema.toDescriptor())
}