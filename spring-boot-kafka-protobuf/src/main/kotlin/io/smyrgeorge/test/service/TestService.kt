package io.smyrgeorge.test.service

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
import io.smyrgeorge.test.proto.domain.Customer.CustomerOuterClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class TestService {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private val schemaRegistryUrl = "http://localhost:58002/apis/ccompat/v7"
    private val schemaRegistryClient: CachedSchemaRegistryClient = CachedSchemaRegistryClient(schemaRegistryUrl, 10)


    private val protobufSerializer = KafkaProtobufSerializer<CustomerOuterClass.Customer>(schemaRegistryClient)
    private val protobufDeserializer = KafkaProtobufDeserializer<CustomerOuterClass.Customer>(schemaRegistryClient)

    init {
        val protobufSchema = ProtobufSchema(CustomerOuterClass.getDescriptor())
        schemaRegistryClient.register("customer", protobufSchema)
    }

    suspend fun test() {
        log.info("Hola!")
    }
}