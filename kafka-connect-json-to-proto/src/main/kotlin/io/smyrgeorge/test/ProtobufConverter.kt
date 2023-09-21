package io.smyrgeorge.test

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.storage.Converter
import java.util.concurrent.ConcurrentHashMap

class ProtobufConverter : Converter {

    private val jsonNodeConverter: JsonNodeConverter = JsonNodeConverter()

    private val schemaRegistryUrl = "http://apicurio:8080/apis/ccompat/v7"
    private val schemaProvider = ProtobufSchemaProvider()
    private val schemaRegistryClient = CachedSchemaRegistryClient(
        /* baseUrls = */ schemaRegistryUrl,
        /* identityMapCapacity = */ 100,
        /* providers = */ listOf(schemaProvider),
        /* originals = */ emptyMap<String, Any>()
    )

    // <topic, message protobuf descriptor>
    private val protoCache = ConcurrentHashMap<String, Descriptors.Descriptor>()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {
        println("[ProtobufConverter] :: Hello!")
        jsonNodeConverter.configure(configs, isKey)
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val json: String = jsonNodeConverter.fromConnectDataToJson(topic, schema, value)
        val descriptor: Descriptors.Descriptor = protoDescriptorOf(topic)
        val builder = DynamicMessage.newBuilder(descriptor)
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder)
        return builder.build().toByteArray()
    }

    private fun protoDescriptorOf(topic: String): Descriptors.Descriptor {
        val cached: Descriptors.Descriptor? = protoCache[topic]
        return if (cached != null) {
            cached
        } else {
            val meta: SchemaMetadata = schemaRegistryClient.getLatestSchemaMetadata(topic)

            val protoSchema = try {
                schemaRegistryClient.getSchemaById(meta.id) as ProtobufSchema
            } catch (e: Exception) {
                throw e
            }

            val descriptor = protoSchema.toDescriptor()
            protoCache[topic] = descriptor
            descriptor
        }
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue {
        error("[ProtobufConverter] :: toConnectData method not supported")
    }
}