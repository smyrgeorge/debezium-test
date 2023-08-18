package io.smyrgeorge.test.api.events

import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverPartition
import reactor.kafka.receiver.ReceiverRecord
import java.time.Instant
import java.time.ZoneId


/**
 * [InventoryConsumer] application using Reactive API for Kafka.
 * To run sample consumer
 *
 *  - Start Zookeeper and Kafka server
 *  - Update [bootstrapServers] and [.TOPIC] if required
 *  - Create a Kafka topic [.TOPIC]
 *  - Send some messages to the topic.
 *  - Run [InventoryConsumer] as Java application with all dependent jars in the CLASSPATH (e.g., from IDE).
 *  - Shutdown Kafka server and Zookeeper when no longer required.
 *
 * https://projectreactor.io/docs/kafka/release/reference/
 * https://github.com/reactor/reactor-kafka/blob/main/reactor-kafka-samples/src/main/java/reactor/kafka/samples/SampleConsumer.java
 *
 */
@Component
class InventoryConsumer {

    private val log = LoggerFactory.getLogger(this::class.java)


    private val bootstrapServers = "localhost:59092"
    private val topic = "dbserver1.inventory.customers"

    private val props: Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.CLIENT_ID_CONFIG to "spring-boot-kafka",
        ConsumerConfig.GROUP_ID_CONFIG to "spring-boot-kafka",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    )

    private val receiverOptions: ReceiverOptions<String, String> = ReceiverOptions.create(props)

    @PostConstruct
    fun setup() {
        receiver()
    }

    private fun receiver(): Disposable {
        val options = receiverOptions.subscription(setOf(topic))
            .addAssignListener { partitions: Collection<ReceiverPartition> ->
                log.debug("onPartitionsAssigned {}", partitions)
            }
            .addRevokeListener { partitions: Collection<ReceiverPartition> ->
                log.debug("onPartitionsRevoked {}", partitions)
            }

        val kafkaFlux = KafkaReceiver.create(options).receive()
        return kafkaFlux.subscribe { record: ReceiverRecord<String, String> ->

            val offset = record.receiverOffset()

            log.info(
                "Received message: topic-partition={} offset={} timestamp={} key={} value={}",
                offset.topicPartition(),
                offset.offset(),
                Instant.ofEpochMilli(record.timestamp()).atZone(ZoneId.of("UTC")),
                record.key(),
                record.value()
            )

            offset.acknowledge()
        }
    }
}