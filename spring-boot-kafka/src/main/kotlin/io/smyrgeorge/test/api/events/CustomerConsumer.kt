package io.smyrgeorge.test.api.events

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.smyrgeorge.test.util.ObjectMapperFactory
import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.Diff
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
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to JsonNodeDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonNodeValueDeserializer::class.java,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    )

    private val receiverOptions: ReceiverOptions<JsonNode, JsonNodeValueDeserializer.Event> =
        ReceiverOptions.create<JsonNode, JsonNodeValueDeserializer.Event>(props)
            .subscription(setOf(topic))

    @PostConstruct
    fun setup() {
        receiver()
    }

    private fun receiver(): Disposable {
        val kafkaFlux = KafkaReceiver.create(receiverOptions).receive()
        return kafkaFlux.subscribe { record ->

            val offset = record.receiverOffset()

            val before: Customer? = record.value().payload.beforeAs(Customer::class.java)
            val after: Customer? = record.value().payload.afterAs(Customer::class.java)
            val diff: Diff = record.value().payload.diff(Customer::class.java)

            log.info(diff.toString())

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

    data class Customer(
        val id: Int,
        val firstName: String,
        val lastName: String,
        val email: String,
        val test: Test
    ) {
        data class Test(
            val a: Int,
            val b: String
        )
    }

    class JsonNodeDeserializer : Deserializer<JsonNode> {

        private val om = ObjectMapperFactory.createSnakeCase()

        override fun deserialize(topic: String, data: ByteArray): JsonNode =
            om.readTree(data)
    }

    class JsonNodeValueDeserializer : Deserializer<JsonNodeValueDeserializer.Event> {

        override fun deserialize(topic: String, data: ByteArray): Event {

            fun JsonNode.deserializeNestedJsonString(): JsonNode = apply {
                fields().forEach {
                    if (it.value.isTextual) {
                        val str = it.value.asText()
                        try {
                            val parsed = om.readTree(str)
                            this as ObjectNode
                            replace(it.key, parsed)
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            // Deserialize to [JsonNode].
            val e = om.readValue(data, InternalEvent::class.java).apply {
                // Deserialize before/after properties.
                payload.before?.deserializeNestedJsonString()
                payload.after?.deserializeNestedJsonString()
            }

            // Transform to the actual event.
            return e.toEvent(topic)
        }

        data class Event(
            val topic: String,
            val schema: Schema,
            val payload: Payload,
        ) {

            @Suppress("unused")
            enum class Operation {
                CREATE,
                UPDATE,
                DELETE,
                READ
            }

            data class Schema(
                val type: String,
                val fields: JsonNode,
                val optional: Boolean,
                val name: String,
                val version: Int
            )

            data class Payload(
                val before: JsonNode?,
                val after: JsonNode?,
                val source: JsonNode,
                val op: Operation,
                val tsMs: Long,
                val transaction: String?
            ) {

                private var b: Any? = null
                private var a: Any? = null

                @Suppress("UNCHECKED_CAST")
                fun <T> beforeAs(clazz: Class<T>): T? {
                    if (b != null) return b as T
                    return before?.let {
                        val res: T = om.convertValue(before, clazz)
                        b = res
                        res
                    }
                }

                @Suppress("UNCHECKED_CAST")
                fun <T> afterAs(clazz: Class<T>): T? {
                    if (a != null) return a as T
                    return after?.let {
                        val res: T = om.convertValue(after, clazz)
                        a = res
                        res
                    }
                }

                fun diff(clazz: Class<*>): Diff =
                    javers.compare(beforeAs(clazz), afterAs(clazz))
            }
        }

        private data class InternalEvent(
            val schema: Event.Schema,
            val payload: InternalPayload,
            val op: String?
        ) {
            fun toEvent(topic: String): Event = Event(
                topic = topic,
                schema = schema,
                payload = payload.toPayload(),
            )

            data class InternalPayload(
                val before: JsonNode?,
                val after: JsonNode?,
                val source: JsonNode,
                val op: String,
                val tsMs: Long,
                val transaction: String?
            ) {

                fun toPayload(): Event.Payload = Event.Payload(
                    before = before,
                    after = after,
                    source = source,
                    op = when (op) {
                        "c" -> Event.Operation.CREATE
                        "r" -> Event.Operation.READ
                        "d" -> Event.Operation.DELETE
                        "u" -> Event.Operation.UPDATE
                        else -> error("Cannot map value='$op' to Operation enum.")
                    },
                    tsMs = tsMs,
                    transaction = transaction
                )
            }
        }

        companion object {
            private val om: ObjectMapper = ObjectMapperFactory.createSnakeCase()
            private val javers: Javers = JaversBuilder.javers()
                .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .build()
        }
    }
}

