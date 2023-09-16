package io.smyrgeorge.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.smyrgeorge.test.domain.Customer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.storage.Converter

@OptIn(ExperimentalSerializationApi::class)
class ProtobufConverter : Converter {

    private val jsonNodeConverter: JsonNodeConverter = JsonNodeConverter()

//    private val classes: Map<String, Class<*>> =
//        ClassLoader("io.smyrgeorge.test.domain").getTypesAnnotatedWith(Dbz::class.java)
//            .associateBy { it.getAnnotation(Dbz::class.java).topic }
//            .map { e -> e.key to e.value.declaredClasses.first { it.simpleName == "ChangeEvent" } }.toMap()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {
        println("[ProtobufConverter] :: Hello!")
        jsonNodeConverter.configure(configs, isKey)
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val jsonNode: JsonNode = jsonNodeConverter.fromConnectDataToJsonNode(topic, schema, value)
        return when (topic) {
            "dbserver1.inventory.customers" -> {
                val e = jsonNodeConverter.om.treeToValue<Customer.ChangeEvent>(jsonNode)
                ProtoBuf.encodeToByteArray(e)
            }

            else -> error("[ProtobufConverter] :: Cannot convert $jsonNode to protobuf. Topic not supported.")
        }
//        val kClass: Class<*> = classes[topic] ?: error("[ProtobufConverter] :: KClass not found. Terminating..")
//        val entity: Any = jsonNodeConverter.om.treeToValue(jsonNode, kClass)
//        println("[ProtobufConverter] :: $entity")
//        return ProtoBuf.encodeToByteArray(entity)
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue {
        error("[ProtobufConverter] :: toConnectData method not supported")
    }
}