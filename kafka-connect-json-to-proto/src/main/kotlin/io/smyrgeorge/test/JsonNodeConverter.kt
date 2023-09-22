package io.smyrgeorge.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.CaseFormat
import io.smyrgeorge.test.util.ObjectMapperFactory
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.storage.Converter
import java.math.BigDecimal
import java.math.BigInteger

class JsonNodeConverter : Converter {

    private val om = ObjectMapperFactory.createCamelCase()

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        println("[JsonNodeConverter] Hello!")
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val jsonNode = fromConnectDataToJsonNode(topic, schema, value)
        return om.writeValueAsBytes(jsonNode)
    }

    @Suppress("UNUSED_PARAMETER")
    fun fromConnectDataToJsonNode(topic: String, schema: Schema, value: Any): JsonNode {
        value as Struct
        return value.toJsonNode()
    }

    fun fromConnectDataToJson(topic: String, schema: Schema, value: Any): String {
        val jsonNode = fromConnectDataToJsonNode(topic, schema, value)
        return om.writeValueAsString(jsonNode)
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue {
        error("[JsonNodeConverter] toConnectData method not supported")
    }

    private fun Struct.toJsonNode(): JsonNode {
        // Create an empty [ObjectNode].
        val node: ObjectNode = om.createObjectNode()

        // Convert each property.
        schema().fields().forEach { f ->
            val name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, f.name())
            when (val v: Any? = get(f)) {
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
                is Struct -> node.set(name, v.toJsonNode())
                else -> error("Cannot map Struct to JsonNode. Value was $v")
            }
        }

        return node.deserializeJsonStringFields()
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
}