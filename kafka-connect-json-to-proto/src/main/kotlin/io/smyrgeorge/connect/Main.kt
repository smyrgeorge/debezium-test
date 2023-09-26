package io.smyrgeorge.connect

import com.google.protobuf.DynamicMessage
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer
import io.smyrgeorge.connect.converter.ProtobufConverter
import org.intellij.lang.annotations.Language

class Main

fun main() {
    val c = ProtobufConverter().apply {
        val configs = mapOf(
            ProtobufConverter.Config.SCHEMA_REGISTRY_URL to "http://localhost:58085",
            ProtobufConverter.Config.USE_LATEST_VERSION to false,
            ProtobufConverter.Config.SKIP_PROPERTIES to "transaction;source.tsMs;source.snapshot;source.sequence;source.txid;source.lsn;source.xmin"
        )
        configure(configs, false)
    }


    val topic = "dbserver1.inventory.customers"

    @Language("json")
    val json = """
        {
        	"before": {
        		"id": 1001,
        		"firstName": "Sally",
        		"lastName": "Thomas",
        		"email": "sally.thomas@acme.comm"
        	},
        	"after": {
        		"id": 1001,
        		"firstName": "Sally",
        		"lastName": "Thomas",
        		"email": "sally.thomas@acme.com"
        	},
        	"source": {
        		"version": "2.4.0.Beta2",
        		"connector": "postgresql",
        		"name": "dbserver1",
        		"db": "postgres",
        		"schema": "inventory",
        		"table": "customers"
        	},
        	"op": "u",
        	"tsMs": 1695335328950
        }""".trimIndent()

    val deserializer = KafkaProtobufDeserializer<DynamicMessage>(c.schemaRegistryClient)

    val p1: ByteArray = c.fromConnectData(topic, json)
    val m1 = deserializer.deserialize(topic, p1)
    println("Message: $m1")

    val p2: ByteArray = c.fromConnectData(topic, json)
    val m2 = deserializer.deserialize(topic, p2)
    println("Message: $m2")
}