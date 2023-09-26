package io.smyrgeorge.connect.util

import com.google.protobuf.Message
import io.confluent.kafka.serializers.protobuf.AbstractKafkaProtobufSerializer
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy
import io.smyrgeorge.connect.converter.Cache
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serializer

class KafkaProtobufSerializer<T : Message>(
    private val subjectNameStrategy: SubjectNameStrategy,
    private val schemaCache: Cache,
) : AbstractKafkaProtobufSerializer<T>(), Serializer<T> {

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        this.isKey = isKey
        configure(KafkaProtobufSerializerConfig(configs))
    }

    override fun serialize(topic: String, data: T): ByteArray =
        serialize(topic, null, data)

    override fun serialize(topic: String, headers: Headers?, record: T): ByteArray {
        val subject = subjectNameStrategy.subjectName(topic, isKey, null)
        val schema = schemaCache[subject] ?: error("Schema for '${topic}' not found.")
        return serializeImpl(subject, topic, isKey, record, schema.second)
    }

    override fun close() {}
}
