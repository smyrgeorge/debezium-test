package io.smyrgeorge.test

import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.storage.Converter
import java.util.concurrent.ConcurrentHashMap

class ProtobufConverter : Converter {

    private val jsonNodeConverter: JsonNodeConverter = JsonNodeConverter()

    private val schemaRegistryUrl = "http://apicurio:8080/apis/ccompat/v7"
    private val schemaRegistryClient = CachedSchemaRegistryClient(schemaRegistryUrl, 100)

    // <topic, dynamic message builder>
    private val protoCache = ConcurrentHashMap<String, DynamicMessage.Builder>()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {
        println("[ProtobufConverter] :: Hello!")
        jsonNodeConverter.configure(configs, isKey)
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val json: String = jsonNodeConverter.fromConnectDataToJson(topic, schema, value)
        val builder: DynamicMessage.Builder = protoDescriptorOf(topic)
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder)
        return builder.build().toByteArray()
    }

    private fun protoDescriptorOf(topic: String): DynamicMessage.Builder {
        val cached: DynamicMessage.Builder? = protoCache[topic]
        return if (cached != null) {
            cached
        } else {
            val meta: SchemaMetadata = schemaRegistryClient.getLatestSchemaMetadata(topic)

            val protoSchema = try {
                schemaRegistryClient.getSchemaBySubjectAndId(topic, meta.id) as ProtobufSchema
            } catch (e: Exception) {
                println(e.message)
                throw e
            }

            val builder = DynamicMessage.newBuilder(protoSchema.toDescriptor())
            protoCache[topic] = builder
            builder
        }
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue {
        error("[ProtobufConverter] :: toConnectData method not supported")
    }
}