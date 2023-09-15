package io.smyrgeorge.test

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.transforms.Transformation

class JsonNodeToProtobufTransformer : Transformation<SourceRecord> {

    override fun config(): ConfigDef = ConfigDef()

    override fun configure(configs: Map<String, *>) {
        println("[JsonNodeToProtobufTransformer] Hello!")
    }

    override fun apply(record: SourceRecord): SourceRecord {
        println("[JsonNodeToProtobufTransformer] received: $record of type='${record::class.java.name}'.")
        return record
    }

    override fun close() {
        println("[JsonNodeToProtobufTransformer] Bye!")
    }
}