package io.smyrgeorge.test.api.events

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
import io.smyrgeorge.test.proto.domain.Customer.CustomerOuterClass
import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.ListCompareAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Instant
import java.time.ZoneId


/**
 * [CustomerConsumer] application using Reactive API for Kafka.
 * To run sample consumer
 *
 *  - Start Zookeeper and Kafka server
 *  - Update [bootstrapServers] and [.TOPIC] if required
 *  - Create a Kafka topic [.TOPIC]
 *  - Send some messages to the topic.
 *  - Run [CustomerConsumer] as Java application with all dependent jars in the CLASSPATH (e.g., from IDE).
 *  - Shutdown Kafka server and Zookeeper when no longer required.
 *
 * https://projectreactor.io/docs/kafka/release/reference/
 * https://github.com/reactor/reactor-kafka/blob/main/reactor-kafka-samples/src/main/java/reactor/kafka/samples/SampleConsumer.java
 *
 *
 * For deep object diff comparison check here:
 * https://javers.org/documentation/diff-examples/
 */
@Component
class CustomerConsumer {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val bootstrapServers = "localhost:59092"
    private val topic = "dbserver1.inventory.customers"

    private val props: Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.CLIENT_ID_CONFIG to "spring-boot-kafka",
        ConsumerConfig.GROUP_ID_CONFIG to "spring-boot-kafka",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    )

    private val receiverOptions: ReceiverOptions<ByteArray, ByteArray> =
        ReceiverOptions.create<ByteArray, ByteArray>(props)
            .subscription(setOf(topic))

    private val schemaRegistryUrl = "http://localhost:58085"
    private val schemaRegistryClient = CachedSchemaRegistryClient(
        /* baseUrls = */ schemaRegistryUrl,
        /* identityMapCapacity = */ 1000,
        /* providers = */ listOf(ProtobufSchemaProvider()),
        /* originals = */ emptyMap<String, Any>()
    )

    private val protobufSerializer = KafkaProtobufSerializer<CustomerOuterClass.Customer>(schemaRegistryClient)

    private val protobufDeserializer = KafkaProtobufDeserializer<CustomerOuterClass.Customer>(schemaRegistryClient)

    @PostConstruct
    fun setup() {
        receiver()
    }

    private fun receiver(): Disposable {
        val kafkaFlux = KafkaReceiver.create(receiverOptions).receive()
        return kafkaFlux.subscribe { record ->

            val offset = record.receiverOffset()

            log.info(
                "Received message: topic-partition={} offset={} timestamp={} key={} value={}",
                offset.topicPartition(),
                offset.offset(),
                Instant.ofEpochMilli(record.timestamp()).atZone(ZoneId.of("UTC")),
                record.key(),
                record.value()
            )

            val value: CustomerOuterClass.Customer = protobufDeserializer.deserialize(topic, record.value())

            offset.acknowledge()
        }
    }

    companion object {
        private val javers: Javers = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
            .build()
    }
}

