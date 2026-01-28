package com.temporal.jetstream.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.temporal.jetstream.model.EnrichedFlightEvent;
import com.temporal.jetstream.model.FlightEvent;
import com.temporal.jetstream.model.FlightEventType;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.LocalDateTime;

/**
 * Flink job that enriches flight events from Kafka before they reach Temporal workflows.
 * <p>
 * Consumes from 'raw-flight-events' topic, enriches with calculated fields, and produces to 'flight-events' topic.
 * </p>
 */
public class FlinkEnrichmentJob {

    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String RAW_EVENTS_TOPIC = "raw-flight-events";
    private static final String ENRICHED_EVENTS_TOPIC = "flight-events";
    private static final String CONSUMER_GROUP_ID = "flink-enrichment-consumer";

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(RAW_EVENTS_TOPIC)
                .setGroupId(CONSUMER_GROUP_ID)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // Create data stream from Kafka source
        DataStream<String> rawEvents = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "Kafka Raw Flight Events Source"
        );

        // Enrich events
        DataStream<String> enrichedEvents = rawEvents
                .map(new FlightEventEnricher())
                .name("Enrich Flight Events");

        // Configure Kafka sink
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(ENRICHED_EVENTS_TOPIC)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .build();

        // Write enriched events to Kafka
        enrichedEvents.sinkTo(sink).name("Kafka Enriched Events Sink");

        // Execute the Flink job
        env.execute("Flight Event Enrichment Job");
    }

    /**
     * MapFunction that enriches raw flight events with calculated fields.
     */
    public static class FlightEventEnricher implements MapFunction<String, String> {
        private static final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @Override
        public String map(String rawEventJson) throws Exception {
            try {
                // Deserialize raw event
                FlightEvent rawEvent = objectMapper.readValue(rawEventJson, FlightEvent.class);

                // Calculate enrichment fields
                Integer estimatedDelay = calculateEstimatedDelay(rawEvent);
                String riskScore = calculateRiskScore(estimatedDelay);
                LocalDateTime enrichedTimestamp = LocalDateTime.now();

                // Create enriched event
                EnrichedFlightEvent enrichedEvent = new EnrichedFlightEvent(
                        rawEvent,
                        estimatedDelay,
                        riskScore,
                        enrichedTimestamp
                );

                // Serialize back to JSON
                return objectMapper.writeValueAsString(enrichedEvent);

            } catch (Exception e) {
                // Log error and return original event (graceful degradation)
                System.err.println("Error enriching event: " + e.getMessage());
                return rawEventJson;
            }
        }

        /**
         * Calculate estimated delay based on event type and data.
         * Simple calculation for demo purposes.
         */
        private Integer calculateEstimatedDelay(FlightEvent event) {
            // For DELAY_ANNOUNCED events, extract delay from data
            if (event.getEventType() == FlightEventType.DELAY_ANNOUNCED && event.getData() != null) {
                try {
                    // Parse data JSON to extract delayMinutes
                    var dataNode = objectMapper.readTree(event.getData());
                    if (dataNode.has("delayMinutes")) {
                        return dataNode.get("delayMinutes").asInt();
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }

            // For other events, return 0 (no delay)
            return 0;
        }

        /**
         * Calculate risk score based on delay threshold.
         * - delay > 60 minutes: HIGH
         * - delay > 30 minutes: MEDIUM
         * - otherwise: LOW
         */
        private String calculateRiskScore(Integer estimatedDelay) {
            if (estimatedDelay == null || estimatedDelay == 0) {
                return "LOW";
            } else if (estimatedDelay > 60) {
                return "HIGH";
            } else if (estimatedDelay > 30) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
    }
}
