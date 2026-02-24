package com.ab.orderservice.bdd;

import com.ab.orderservice.kafka.TradeEventsProducer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * This class connects Cucumber to Spring Boot.
 * Without it:
 * - Cucumber creates step classes, but Spring won't inject @Autowired beans.
 * - You will get null injection / no application context.
 */

@CucumberContextConfiguration
// - Tells Cucumber-Spring: "This is the class that configures the Spring test context for Cucumber."
@SpringBootTest
// - Starts the full Spring Boot application context (similar to running the app).
// - Allows @Autowired to work, repositories to load, DB to connect, etc.
@ActiveProfiles("test")
// - Uses application-test.properties (or yml) from src/test/resources
// - Lets you turn off Eureka/logging/use H2/etc only in tests (recommended).
public class CucumberSpringConfig {
    /**
     * Replace the real TradeEventsProducer bean with a Mockito mock for ALL scenarios.
     * Why do we inject/mock it here (instead of in the step class)?
     * - TradeEventsProducer is a Spring @Component that uses KafkaTemplate internally.
     * - In tests we do NOT want to hit a real Kafka broker or require topics to exist.
     * - If the real producer is used, MatchingService.match(...) will call publish(...),
     *   which triggers kafkaTemplate.send(...) -> causes ---"Kafka send failed"--- in tests.
     *
     * Putting this override here ensures:
     * - The Spring context starts with a mocked TradeEventsProducer.
     * - Any service that autowires TradeEventsProducer (like MatchingService) receives the mock.
     */
    @MockitoBean
    private TradeEventsProducer tradeEventsProducer;
}
