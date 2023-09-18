package io.smyrgeorge.test

import com.fasterxml.jackson.databind.JsonNode
import io.smyrgeorge.test.domain.ProtoBufSerializable
import io.smyrgeorge.test.domain.dbz.ChangeEvent
import io.smyrgeorge.test.domain.dbz.Dbz
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.storage.Converter
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

class ProtobufConverter : Converter {

    private val jsonNodeConverter: JsonNodeConverter = JsonNodeConverter()

    private val classes: Map<String, Class<*>> =
        ClassLoader("io.smyrgeorge.test.domain").getTypesAnnotatedWith(Dbz::class.java)
            .associateBy { it.getAnnotation(Dbz::class.java).topic }
            .map { e -> e.key to e.value.declaredClasses.first { it.simpleName == ChangeEvent::class.simpleName } }
            .toMap()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {
        println("[ProtobufConverter] :: Hello!")
        jsonNodeConverter.configure(configs, isKey)
    }

    override fun fromConnectData(topic: String, schema: Schema, value: Any): ByteArray {
        val jsonNode: JsonNode = jsonNodeConverter.fromConnectDataToJsonNode(topic, schema, value)
        val kClass: Class<*> = classes[topic] ?: error("[ProtobufConverter] :: KClass not found. Terminating..")
        val entity: ProtoBufSerializable = jsonNodeConverter.om.treeToValue(jsonNode, kClass) as ProtoBufSerializable
        println("[ProtobufConverter] :: $entity")
        return entity.toProtoBuf()
    }

    override fun toConnectData(topic: String, value: ByteArray): SchemaAndValue {
        error("[ProtobufConverter] :: toConnectData method not supported")
    }

    private class ClassLoader(packageName: String) {

        private val configuration = ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(packageName))
            .addScanners(Scanners.TypesAnnotated)

        private val reflections = Reflections(configuration)

        fun getTypesAnnotatedWith(annotation: Class<out Annotation>): Set<Class<*>> =
            reflections.getTypesAnnotatedWith(annotation)
    }
}