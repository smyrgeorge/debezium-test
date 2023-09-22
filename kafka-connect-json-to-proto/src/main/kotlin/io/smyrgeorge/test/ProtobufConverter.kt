package io.smyrgeorge.test

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import io.confluent.kafka.schemaregistry.utils.BoundedConcurrentHashMap
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig
import io.confluent.kafka.serializers.subject.TopicNameStrategy
import io.smyrgeorge.test.util.KafkaProtobufSerializer
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.storage.Converter

class ProtobufConverter : Converter {

    private var isKey: Boolean = false
    private lateinit var schemaRegistryUrl: String
    private var schemaRegistryCacheCapacity: Int = 1000
    private lateinit var schemaRegistryClient: SchemaRegistryClient
    private val subjectNameStrategy = TopicNameStrategy()
    private val jsonNodeConverter: JsonNodeConverter = JsonNodeConverter()

    // <subject, message protobuf descriptor>
    private val schemaCache: MutableMap<String, Pair<Descriptors.Descriptor, ProtobufSchema>> =
        BoundedConcurrentHashMap()
    private lateinit var serializer: KafkaProtobufSerializer<DynamicMessage>

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        println("[ProtobufConverter] :: Hello! $configs")
        jsonNodeConverter.configure(configs, isKey)

        this.isKey = isKey

        schemaRegistryUrl = configs[Config.SCHEMA_REGISTRY_URL] as String?
            ?: error("${Config.SCHEMA_REGISTRY_URL} config property was null.")

        configs[Config.SCHEMA_REGISTRY_CACHE_CAPACITY]?.let {
            val value = if (it is String) it.toInt() else it as Int
            schemaRegistryCacheCapacity = value
        }

        schemaRegistryClient = CachedSchemaRegistryClient(
            /* baseUrls = */ schemaRegistryUrl,
            /* identityMapCapacity = */ schemaRegistryCacheCapacity,
            /* providers = */ listOf(ProtobufSchemaProvider()),
            /* originals = */ emptyMap<String, Any>()
        )

        serializer = KafkaProtobufSerializer<DynamicMessage>(subjectNameStrategy, schemaCache).apply {
            val conf = mapOf<String, Any>(
                KafkaProtobufSerializerConfig.USE_LATEST_VERSION to true,
                KafkaProtobufSerializerConfig.AUTO_REGISTER_SCHEMAS to false,
                KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
            )
            configure(conf, isKey)
        }
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val json: String = jsonNodeConverter.fromConnectDataToJson(topic, schema, value)
        return fromConnectData(topic, json)
    }

    fun fromConnectData(topic: String, json: String): ByteArray {
        println("[ProtobufConverter]: $json")

        val descriptor: Descriptors.Descriptor = protoSchemaOf(topic).first
        val builder: DynamicMessage.Builder = DynamicMessage.newBuilder(descriptor)
        // TODO: remove ignoringUnknownFields()
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder)
        val message: DynamicMessage = builder.build()
        return serializer.serialize(topic, message)
    }

    private fun protoSchemaOf(topic: String): Pair<Descriptors.Descriptor, ProtobufSchema> {
        val subject = subjectNameStrategy.subjectName(topic, isKey, null)
        val cached = schemaCache[subject]
        // TODO: check if expired
        return if (cached != null) {
            cached
        } else {
            val meta: SchemaMetadata = schemaRegistryClient.getLatestSchemaMetadata(subject)
            val schema = schemaRegistryClient.getSchemaBySubjectAndId(subject, meta.id) as ProtobufSchema
            val descriptor: Descriptors.Descriptor = schema.toDescriptor()
            val pair = descriptor to schema
            schemaCache[subject] = pair
            pair
        }
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue =
        error("[ProtobufConverter] :: toConnectData method not supported")

    object Config {
        const val SCHEMA_REGISTRY_URL: String = "protobuf.schema.registry.url"
        const val SCHEMA_REGISTRY_CACHE_CAPACITY: String = "protobuf.schema.cache.capacity"
    }
}