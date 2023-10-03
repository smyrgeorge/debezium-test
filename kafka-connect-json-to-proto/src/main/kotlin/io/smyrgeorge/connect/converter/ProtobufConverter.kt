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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

typealias Cache = MutableMap<String, Triple<Descriptors.Descriptor, ProtobufSchema, Instant>>

class ProtobufConverter : Converter {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private var isKey: Boolean = false
    private var useLatestVersion: Boolean = false
    private lateinit var schemaRegistryUrl: String
    private var schemaRegistryCacheCapacity: Int = 1000
    private var schemaRegistryCacheExpiryMinutes: Duration = 10.minutes.toJavaDuration()
    private val subjectNameStrategy = TopicNameStrategy()
    lateinit var schemaRegistryClient: SchemaRegistryClient
    private val jsonNodeConverter = JsonNodeConverter()

    // <subject, triple<message protobuf descriptor, schema, expiry>>
    private val cache: Cache = BoundedConcurrentHashMap()
    private lateinit var serializer: KafkaProtobufSerializer<DynamicMessage>

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        log.info("Hola! from ProtobufConverter! :: $configs")

        // Configure [JsonNodeConverter].
        jsonNodeConverter.configure(configs.toJsonNodeConverterProps(), isKey)

        this.isKey = isKey

        configs[Config.USE_LATEST_VERSION]?.let {
            useLatestVersion = if (it is String) it.toBoolean() else it as Boolean
        }

        schemaRegistryUrl = configs[Config.SCHEMA_REGISTRY_URL] as String?
            ?: error("${Config.SCHEMA_REGISTRY_URL} config property was null.")

        configs[Config.SCHEMA_REGISTRY_CACHE_CAPACITY]?.let {
            schemaRegistryCacheCapacity = if (it is String) it.toInt() else it as Int
        }

        configs[Config.SCHEMA_REGISTRY_CACHE_EXPIRY_MINUTES]?.let {
            val value = if (it is String) it.toInt().minutes else (it as Int).minutes
            schemaRegistryCacheExpiryMinutes = value.toJavaDuration()
        }

        schemaRegistryClient = CachedSchemaRegistryClient(
            /* baseUrls = */ schemaRegistryUrl,
            /* identityMapCapacity = */ schemaRegistryCacheCapacity,
            /* providers = */ listOf(ProtobufSchemaProvider()),
            /* originals = */ emptyMap<String, Any>()
        )

        serializer = KafkaProtobufSerializer<DynamicMessage>(subjectNameStrategy, cache).apply {
            val conf = mapOf<String, Any>(
                KafkaProtobufSerializerConfig.AUTO_REGISTER_SCHEMAS to false,
                KafkaProtobufSerializerConfig.USE_LATEST_VERSION to useLatestVersion,
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

    private fun protoSchemaOf(topic: String): Triple<Descriptors.Descriptor, ProtobufSchema, Instant> {

        fun Instant.isExpired(): Boolean =
            isAfter(Instant.now().plus(schemaRegistryCacheExpiryMinutes))

        val subject = subjectNameStrategy.subjectName(topic, isKey, null)

        var cached = cache[subject]
        if (cached != null && cached.third.isExpired()) {
            cached = null
            cache.remove(subject)
        }

        return if (cached != null) {
            cached
        } else {
            val meta: SchemaMetadata = schemaRegistryClient.getLatestSchemaMetadata(subject)
            val schema = schemaRegistryClient.getSchemaBySubjectAndId(subject, meta.id) as ProtobufSchema
            val descriptor: Descriptors.Descriptor = schema.toDescriptor()
            val triple = Triple(descriptor, schema, Instant.now())
            cache[subject] = triple
            triple
        }
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue =
        error("ProtobufConverter :: toConnectData method not supported")

    private fun Map<String, *>.toJsonNodeConverterProps(): Map<String, *> {
        val configs: MutableMap<String, Any> = mutableMapOf()

        this[Config.SKIP_PROPERTIES]?.let {
            configs[JsonNodeConverter.Config.SKIP_PROPERTIES] = it
        }

        this[Config.CONVERT_DATES_MODE]?.let {
            configs[JsonNodeConverter.Config.CONVERT_DATES_MODE] = it
        }

        this[Config.CONVERT_DATES_WITH_PREFIX]?.let {
            configs[JsonNodeConverter.Config.CONVERT_DATES_WITH_PREFIX] = it
        }

        this[Config.CONVERT_DATES_WITH_SUFFIX]?.let {
            configs[JsonNodeConverter.Config.CONVERT_DATES_WITH_SUFFIX] = it
        }

        return configs
    }

    object Config {
        const val SCHEMA_REGISTRY_URL: String = "protobuf.schema.registry.url"
        const val SCHEMA_REGISTRY_CACHE_CAPACITY: String = "protobuf.schema.cache.capacity"
        const val SCHEMA_REGISTRY_CACHE_EXPIRY_MINUTES: String = "protobuf.schema.cache.expiry.minutes"
        const val USE_LATEST_VERSION: String = "protobuf.use.latest.version"

        const val SKIP_PROPERTIES: String = "protobuf.json.exclude.properties"
        const val CONVERT_DATES_MODE: String = "protobuf.json.convert.dates.mode"
        const val CONVERT_DATES_WITH_PREFIX: String = "protobuf.json.convert.dates.with.prefix"
        const val CONVERT_DATES_WITH_SUFFIX: String = "protobuf.json.convert.dates.with.suffix"
    }
}