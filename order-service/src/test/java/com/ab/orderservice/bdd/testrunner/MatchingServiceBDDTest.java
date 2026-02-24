package com.ab.orderservice.bdd.testrunner;

import io.cucumber.junit.Cucumber;  // JUnit runner provided by Cucumber
import io.cucumber.junit.CucumberOptions; // Configuration for features, glue, plugins
import org.junit.runner.RunWith;

/**
 * This is the test runner class:
 * - JUnit will run this class
 * - Cucumber will read the options and execute .feature files
 */
@RunWith(Cucumber.class)
// Meaning:
// - Instead of running normal JUnit tests, JUnit will delegate to Cucumber runner.
// - Cucumber will find scenarios and run them as tests.
@CucumberOptions(
        /**
         * Where Cucumber should search for .feature files.
         * You placed them in src/test/java/... (it works),
         * but standard location is src/test/resources/features.
        */
        features = {"src/test/java/com/ab/orderservice/bdd/features"},
        /**
         * "glue" = packages where Cucumber searches for:
         * - step definition classes (TradeSteps)
         * - hooks (Before/After)
         * - cucumber spring configuration class (CucumberSpringConfig)
         *
         * If Cucumber can't find @CucumberContextConfiguration in glue,
         * !!!! you get the error.
        */
        glue = {
                "com.ab.orderservice.bdd",        // to pick up CucumberSpringConfig
                "com.ab.orderservice.bdd.steps"   // to pick up step defs
        },
        /**
         * Output formatting.
         * "pretty" prints readable scenario execution output.
        */
        plugin = {"pretty"},
        monochrome = true // used to make the console output of your test execution cleaner and more readable.
        /**
         * Optional:::
         * tags = "@smoke"   -> run only scenarios with that tag
         * dryRun = true     -> validates steps exist without executing code
        */
)
public class MatchingServiceBDDTest {
}

