package io.smyrgeorge.connect.converter

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
import io.smyrgeorge.connect.util.KafkaProtobufSerializer
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.storage.Converter

class ProtobufConverter : Converter {

    private var isKey: Boolean = false
    private lateinit var schemaRegistryUrl: String
    private var schemaRegistryCacheCapacity: Int = 1000
    private var autoRegisterSchemas: Boolean = false
    private var useLatestVersion: Boolean = true

    lateinit var schemaRegistryClient: SchemaRegistryClient
    private val subjectNameStrategy = TopicNameStrategy()
    val jsonNodeConverter: JsonNodeConverter = JsonNodeConverter()

    // <subject, message protobuf descriptor>
    private val cache: MutableMap<String, Pair<Descriptors.Descriptor, ProtobufSchema>> = BoundedConcurrentHashMap()
    private lateinit var serializer: KafkaProtobufSerializer<DynamicMessage>

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        println("[ProtobufConverter] :: Hola! $configs")

        val jsonProps = configs[Config.SKIP_PROPERTIES]?.let {
            mapOf(JsonNodeConverter.Config.SKIP_PROPERTIES to it as String)
        } ?: emptyMap()
        jsonNodeConverter.configure(jsonProps, isKey)

        this.isKey = isKey

        schemaRegistryUrl = configs[Config.SCHEMA_REGISTRY_URL] as String?
            ?: error("${Config.SCHEMA_REGISTRY_URL} config property was null.")

        configs[Config.SCHEMA_REGISTRY_CACHE_CAPACITY]?.let {
            schemaRegistryCacheCapacity = if (it is String) it.toInt() else it as Int
        }

        configs[Config.AUTO_REGISTER_SCHEMAS]?.let {
            autoRegisterSchemas = if (it is String) it.toBoolean() else it as Boolean
        }

        configs[Config.USE_LATEST_VERSION]?.let {
            useLatestVersion = if (it is String) it.toBoolean() else it as Boolean
        }

        schemaRegistryClient = CachedSchemaRegistryClient(
            /* baseUrls = */ schemaRegistryUrl,
            /* identityMapCapacity = */ schemaRegistryCacheCapacity,
            /* providers = */ listOf(ProtobufSchemaProvider()),
            /* originals = */ emptyMap<String, Any>()
        )

        serializer = KafkaProtobufSerializer<DynamicMessage>(subjectNameStrategy, cache).apply {
            val conf = mapOf<String, Any>(
                KafkaProtobufSerializerConfig.USE_LATEST_VERSION to useLatestVersion,
                KafkaProtobufSerializerConfig.AUTO_REGISTER_SCHEMAS to autoRegisterSchemas,
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
        val descriptor: Descriptors.Descriptor = protoSchemaOf(topic).first
        val builder: DynamicMessage.Builder = DynamicMessage.newBuilder(descriptor)
        JsonFormat.parser().merge(json, builder)
        val message: DynamicMessage = builder.build()
        return serializer.serialize(topic, message)
    }

    private fun protoSchemaOf(topic: String): Pair<Descriptors.Descriptor, ProtobufSchema> {
        val subject = subjectNameStrategy.subjectName(topic, isKey, null)
        val cached = cache[subject]
        // TODO: check if expired
        return if (cached != null) {
            cached
        } else {
            val meta: SchemaMetadata = schemaRegistryClient.getLatestSchemaMetadata(subject)
            val schema = schemaRegistryClient.getSchemaBySubjectAndId(subject, meta.id) as ProtobufSchema
            val descriptor: Descriptors.Descriptor = schema.toDescriptor()
            val pair = descriptor to schema
            cache[subject] = pair
            pair
        }
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue =
        error("[ProtobufConverter] :: toConnectData method not supported")

    object Config {
        const val SCHEMA_REGISTRY_URL: String = "protobuf.schema.registry.url"
        const val SCHEMA_REGISTRY_CACHE_CAPACITY: String = "protobuf.schema.cache.capacity"
        const val AUTO_REGISTER_SCHEMAS: String = "protobuf.auto.register.schemas"
        const val USE_LATEST_VERSION: String = "protobuf.use.latest.version"
        const val SKIP_PROPERTIES: String = "protobuf.json.exclude.properties"
    }
}