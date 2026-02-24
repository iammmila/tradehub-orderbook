package com.ab.tradeservice.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
}
