package io.smyrgeorge.test

class Main

fun main() {
    val converter = ProtobufConverter().apply {
        val configs = mapOf(
            ProtobufConverter.Config.SCHEMA_REGISTRY_URL to "http://localhost:58085"
        )
        configure(configs, false)
    }

    val topic = "dbserver1.inventory.customers"
    val json = """{"before":{"id":1001,"firstName":"Sally","lastName":"Thomas","email":"sally.thomas@acme.comm"},"after":{"id":1001,"firstName":"Sally","lastName":"Thomas","email":"sally.thomas@acme.com"},"source":{"version":"2.4.0.Beta2","connector":"postgresql","name":"dbserver1","tsMs":1695335029254,"snapshot":false,"db":"postgres","sequence":["35491400","35492272"],"schema":"inventory","table":"customers","txid":869,"lsn":35492272,"xmin":null},"op":"u","tsMs":1695335328950,"transaction":null}""".trimIndent()

    val p1 = converter.fromConnectData(topic, json)
    val p2 = converter.fromConnectData(topic, json)
    val p3 = converter.fromConnectData(topic, json)
}