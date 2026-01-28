package com.temporal.jetstream.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.temporal.jetstream.model.FlightEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Flink enrichment logic.
 */
class FlinkEnrichmentJobTest {

    private FlinkEnrichmentJob.FlightEventEnricher enricher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        enricher = new FlinkEnrichmentJob.FlightEventEnricher();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void testEnrichDelayEvent() throws Exception {
        // Given: A raw delay announcement event
        String rawEvent = """
                {
                  "eventType": "DELAY_ANNOUNCED",
                  "flightNumber": "AA1234",
                  "flightDate": "2026-01-27",
                  "data": "{\\"delayMinutes\\":45}"
                }
                """;

        // When: Event is enriched
        String enrichedJson = enricher.map(rawEvent);

        // Then: Enriched event contains calculated fields
        JsonNode enriched = objectMapper.readTree(enrichedJson);
        assertEquals("DELAY_ANNOUNCED", enriched.get("eventType").asText());
        assertEquals("AA1234", enriched.get("flightNumber").asText());
        assertEquals("2026-01-27", enriched.get("flightDate").asText());
        assertEquals(45, enriched.get("estimatedDelay").asInt());
        assertEquals("MEDIUM", enriched.get("riskScore").asText()); // 45 > 30, so MEDIUM
        assertNotNull(enriched.get("enrichedTimestamp"));
    }

    @Test
    void testEnrichHighRiskDelay() throws Exception {
        // Given: A high delay event (> 60 minutes)
        String rawEvent = """
                {
                  "eventType": "DELAY_ANNOUNCED",
                  "flightNumber": "DL5678",
                  "flightDate": "2026-01-27",
                  "data": "{\\"delayMinutes\\":90}"
                }
                """;

        // When: Event is enriched
        String enrichedJson = enricher.map(rawEvent);

        // Then: Risk score should be HIGH
        JsonNode enriched = objectMapper.readTree(enrichedJson);
        assertEquals(90, enriched.get("estimatedDelay").asInt());
        assertEquals("HIGH", enriched.get("riskScore").asText()); // 90 > 60, so HIGH
    }

    @Test
    void testEnrichLowRiskDelay() throws Exception {
        // Given: A small delay event (< 30 minutes)
        String rawEvent = """
                {
                  "eventType": "DELAY_ANNOUNCED",
                  "flightNumber": "UA9876",
                  "flightDate": "2026-01-27",
                  "data": "{\\"delayMinutes\\":15}"
                }
                """;

        // When: Event is enriched
        String enrichedJson = enricher.map(rawEvent);

        // Then: Risk score should be LOW
        JsonNode enriched = objectMapper.readTree(enrichedJson);
        assertEquals(15, enriched.get("estimatedDelay").asInt());
        assertEquals("LOW", enriched.get("riskScore").asText()); // 15 < 30, so LOW
    }

    @Test
    void testEnrichGateChangeEvent() throws Exception {
        // Given: A gate change event (no delay)
        String rawEvent = """
                {
                  "eventType": "GATE_CHANGED",
                  "flightNumber": "SW1111",
                  "flightDate": "2026-01-27",
                  "data": "{\\"gate\\":\\"B15\\"}"
                }
                """;

        // When: Event is enriched
        String enrichedJson = enricher.map(rawEvent);

        // Then: Estimated delay should be 0, risk score LOW
        JsonNode enriched = objectMapper.readTree(enrichedJson);
        assertEquals("GATE_CHANGED", enriched.get("eventType").asText());
        assertEquals(0, enriched.get("estimatedDelay").asInt());
        assertEquals("LOW", enriched.get("riskScore").asText());
    }

    @Test
    void testEnrichEventWithMalformedData() throws Exception {
        // Given: An event with malformed data field
        String rawEvent = """
                {
                  "eventType": "DELAY_ANNOUNCED",
                  "flightNumber": "AA1234",
                  "flightDate": "2026-01-27",
                  "data": "invalid json"
                }
                """;

        // When: Event is enriched
        String enrichedJson = enricher.map(rawEvent);

        // Then: Enrichment should gracefully handle error and default to 0 delay
        JsonNode enriched = objectMapper.readTree(enrichedJson);
        assertEquals(0, enriched.get("estimatedDelay").asInt());
        assertEquals("LOW", enriched.get("riskScore").asText());
    }

    @Test
    void testEnrichEventWithNoData() throws Exception {
        // Given: An event with null data field
        String rawEvent = """
                {
                  "eventType": "BOARDING_STARTED",
                  "flightNumber": "AA1234",
                  "flightDate": "2026-01-27",
                  "data": null
                }
                """;

        // When: Event is enriched
        String enrichedJson = enricher.map(rawEvent);

        // Then: Should work with defaults
        JsonNode enriched = objectMapper.readTree(enrichedJson);
        assertEquals("BOARDING_STARTED", enriched.get("eventType").asText());
        assertEquals(0, enriched.get("estimatedDelay").asInt());
        assertEquals("LOW", enriched.get("riskScore").asText());
    }

    @Test
    void testRiskScoreThresholds() throws Exception {
        // Test boundary conditions for risk scores

        // 30 minutes exactly -> MEDIUM
        String event30 = createDelayEvent(30);
        JsonNode enriched30 = objectMapper.readTree(enricher.map(event30));
        assertEquals("LOW", enriched30.get("riskScore").asText()); // 30 is not > 30

        // 31 minutes -> MEDIUM
        String event31 = createDelayEvent(31);
        JsonNode enriched31 = objectMapper.readTree(enricher.map(event31));
        assertEquals("MEDIUM", enriched31.get("riskScore").asText());

        // 60 minutes exactly -> MEDIUM
        String event60 = createDelayEvent(60);
        JsonNode enriched60 = objectMapper.readTree(enricher.map(event60));
        assertEquals("MEDIUM", enriched60.get("riskScore").asText()); // 60 is not > 60

        // 61 minutes -> HIGH
        String event61 = createDelayEvent(61);
        JsonNode enriched61 = objectMapper.readTree(enricher.map(event61));
        assertEquals("HIGH", enriched61.get("riskScore").asText());
    }

    private String createDelayEvent(int delayMinutes) {
        return String.format("""
                {
                  "eventType": "DELAY_ANNOUNCED",
                  "flightNumber": "TEST123",
                  "flightDate": "2026-01-27",
                  "data": "{\\"delayMinutes\\":%d}"
                }
                """, delayMinutes);
    }
}
