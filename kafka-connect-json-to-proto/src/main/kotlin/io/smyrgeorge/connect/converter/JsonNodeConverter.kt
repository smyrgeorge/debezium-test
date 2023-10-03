package io.smyrgeorge.connect.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.CaseFormat
import io.smyrgeorge.connect.util.ObjectMapperFactory
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.storage.Converter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class JsonNodeConverter : Converter {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val om = ObjectMapperFactory.createCamelCase()

    // set<pair<path, propertyName>>
    private lateinit var skipProperties: Set<Pair<String, String>>

    private var convertDatesWithPrefix: String? = null
    private var convertDatesWithSuffix: String? = null
    private var convertDatesMode: DateConvertor.Mode = DateConvertor.Mode.NONE

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        log.info("Hola from JsonNodeConverter! :: $configs")
        skipProperties = configs[Config.SKIP_PROPERTIES]?.let { c ->
            (c as String)
                .split(',')
                .map {
                    val l = it.split('.')
                    val p = if (l.size == 1) "" else '/' + l.dropLast(1).joinToString("/")
                    p to l.last()
                }.toSet()
        } ?: emptySet()

        convertDatesWithPrefix = configs[Config.CONVERT_DATES_WITH_PREFIX] as String?
        convertDatesWithSuffix = configs[Config.CONVERT_DATES_WITH_SUFFIX] as String?
        convertDatesMode = configs[Config.CONVERT_DATES_MODE]?.let {
            DateConvertor.Mode.valueOf(it as String)
        } ?: DateConvertor.Mode.NONE
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val jsonNode: JsonNode = fromConnectDataToJsonNode(topic, schema, value)
        return om.writeValueAsBytes(jsonNode)
    }

    @Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
    fun fromConnectDataToJsonNode(topic: String, schema: Schema, value: Any): JsonNode {
        value as Struct
        return value.toJsonNode()
    }

    fun fromConnectDataToJson(topic: String, schema: Schema, value: Any): String {
        val jsonNode: JsonNode = fromConnectDataToJsonNode(topic, schema, value)
        return om.writeValueAsString(jsonNode)
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue =
        error("JsonNodeConverter :: toConnectData method not supported")

    private fun Struct.toJsonNode(): JsonNode {
        // Create an empty [ObjectNode].
        val node: ObjectNode = om.createObjectNode()

        // Convert each property.
        schema().fields().forEach { f ->
            val name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, f.name())

            val v: Any? = when {
                convertDatesWithPrefix != null && name.startsWith(convertDatesWithPrefix!!) -> {
                    DateConvertor.format(convertDatesMode, get(f))
                }

                convertDatesWithSuffix != null && name.endsWith(convertDatesWithSuffix!!) -> {
                    DateConvertor.format(convertDatesMode, get(f))
                }

                else -> get(f)
            }

            when (v) {
                null -> node.set(name, NullNode.instance)
                is Int -> node.put(name, v)
                is Long -> node.put(name, v)
                is Short -> node.put(name, v)
                is BigInteger -> node.put(name, v)
                is Double -> node.put(name, v)
                is Float -> node.put(name, v)
                is BigDecimal -> node.put(name, v)
                is String -> node.put(name, v)
                is Boolean -> node.put(name, v)
                is ByteArray -> node.put(name, v)
                is Struct -> node.set(name, v.toJsonNode())
                is Map<*, *>, is List<*>, is Set<*>, is Array<*> -> node.set(name, om.valueToTree(v))
                else -> error("Cannot map ${v::class.simpleName} to JsonNode.")
            }
        }

        return node
            .skipProperties()
            .deserializeJsonStringFields()
    }

    private fun ObjectNode.skipProperties(): ObjectNode = apply {
        skipProperties.forEach {
            if (it.first.isEmpty()) {
                remove(it.second)
            } else {
                when (val n = at(it.first)) {
                    is ObjectNode -> n.remove(it.second)
                    else -> Unit
                }
            }
        }
    }

    private fun JsonNode.deserializeJsonStringFields(): JsonNode = apply {
        fields().forEach { f ->
            when {
                f.value.isObject -> f.value.deserializeJsonStringFields()
                f.value.isArray -> f.value.forEach { it.deserializeJsonStringFields() }
                f.value.isTextual -> {
                    val str = f.value.asText()
                    try {
                        val parsed = om.readTree(str)
                        this as ObjectNode
                        replace(f.key, parsed)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    object Config {
        const val SKIP_PROPERTIES: String = "json.exclude.properties"
        const val CONVERT_DATES_MODE: String = "json.convert.dates.mode"
        const val CONVERT_DATES_WITH_PREFIX: String = "json.convert.dates.with.prefix"
        const val CONVERT_DATES_WITH_SUFFIX: String = "json.convert.dates.with.suffix"
    }

    object DateConvertor {

        fun format(mode: Mode, value: Any): Any =
            when (mode) {
                Mode.FROM_UNIX_MICROSECONDS_TO_RFC_3339 -> fromUnixMicrosToRfc3339(value as Long)
                Mode.NONE -> value
            }

        private val rfc3339Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        fun fromUnixMicrosToRfc3339(micros: Long): String {
            val i = Instant.EPOCH.plus(micros, ChronoUnit.MICROS)
            val z = ZonedDateTime.ofInstant(i, ZoneId.of("UTC"))
            return z.format(rfc3339Formatter)
        }

        enum class Mode {
            NONE,
            FROM_UNIX_MICROSECONDS_TO_RFC_3339
        }
    }
}