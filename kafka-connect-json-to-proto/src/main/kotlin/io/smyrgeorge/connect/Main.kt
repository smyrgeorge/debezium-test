package io.smyrgeorge.connect

import com.google.protobuf.DynamicMessage
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer
import io.smyrgeorge.connect.converter.ProtobufConverter

class Main

fun main() {
    val c1 = ProtobufConverter().apply {
        val configs = mapOf(
            ProtobufConverter.Config.SCHEMA_REGISTRY_URL to "http://localhost:58085",
            ProtobufConverter.Config.SKIP_PROPERTIES to "transaction;source.tsMs;source.snapshot;source.sequence;source.txid;source.lsn;source.xmin"
        )
        configure(configs, false)
    }


    val topic = "dbserver1.inventory.customers"
    val json = """{"before":{"id":1001,"firstName":"Sally","lastName":"Thomas","email":"sally.thomas@acme.comm"},"after":{"id":1001,"firstName":"Sally","lastName":"Thomas","email":"sally.thomas@acme.com"},"source":{"version":"2.4.0.Beta2","connector":"postgresql","name":"dbserver1","tsMs":1695335029254,"snapshot":false,"db":"postgres","sequence":["35491400","35492272"],"schema":"inventory","table":"customers","txid":869,"lsn":35492272,"xmin":null},"op":"u","tsMs":1695335328950,"transaction":null}""".trimIndent()
    val p1: ByteArray = c1.fromConnectData(topic, json)
    val deserializer = KafkaProtobufDeserializer<DynamicMessage>(c1.schemaRegistryClient)
    val message = deserializer.deserialize(topic, p1)
    println("Message: $message")
}