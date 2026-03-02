package com.ab.orderservice.bdd.steps;

import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.*;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.MatchingService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * - GIVEN: insert "resting" orders into the database (OrderRepository)
 * - WHEN: a new order arrives (call MatchingService.match)
 * - THEN: verify order statuses/remaining qty in DB and verify trade event publish was called
 * Important:
 * - Do NOT annotate step classes with @Component/@Service.
 * Cucumber constructs them, and Spring auto-scanning can create duplicates.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// (each scenario starts clean).
public class MatchingSteps {

    /**
     * MatchingService is the real business logic under test.
     * We call matchingService.match(...) to simulate how the app matches orders.
     */
    @Autowired
    private MatchingService matchingService;
    /**
     * OrderRepository is used to:
     * - seed resting orders (Given step)
     * - verify final state (Then steps)
     */
    @Autowired
    private OrderRepository orderRepository;
    /**
     * TradeEventsProducer is injected as a bean, BUT in tests it is a Mockito override
     * because declared @MockitoBean in CucumberSpringConfig.
     * That means:
     * - verify(tradeEventsProducer).publish(...) works
     * - and no real Kafka send is executed.
     */
    @Autowired
    private TradeEventsProducer tradeEventsProducer;
    /**
     * EntityManager is used to manage persistence context (first-level cache).
     * After running matching logic, clearing EntityManager forces the next repository read
     * to come fresh from the database, not from cached entities.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * TransactionTemplate allows us to run code inside a transaction manually.
     * - MatchingService.match(...) is typically @Transactional.
     * - In BDD steps, you want deterministic commit behavior before asserting results.
     * - Using TransactionTemplate ensures the match executes in one transaction and commits
     * before you do DB reads in Then steps.
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Holds the incoming order created in the WHEN step.
     * We check its status and remainingQuantity after matching.
     */
    private Order incomingOrder;

    @Given("the following resting orders exist in the book:")
    public void existingOrders(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        transactionTemplate.execute(tx -> {
            // Convert table rows to real Order entities and persist them
            List<Order> restingOrders = rows.stream().map(r -> {
                Order o = new Order();
                Long id = parseLongOrNull(r.get("id"));
                o.setSide(OrderSide.valueOf(req(r, "side")));
                long qty = Long.parseLong(req(r, "quantity"));
                o.setQuantity(qty);
                o.setRemainingQuantity(qty);
                o.setRoutingMode(RoutingMode.MANUAL);
                o.setRoutedBy(RoutedBy.USER);
                o.setPrice(new BigDecimal(req(r, "price")));
                o.setInstrument(req(r, "instrument"));
                o.setUserId(Long.parseLong(req(r, "userId")));
                o.setStatus(OrderStatus.valueOf(req(r, "status")));
                o.setExchangeCode(req(r, "exchangeCode"));
                o.setType(OrderType.valueOf(req(r, "type")));
                o.setVisible(o.getType() != OrderType.HIDDEN_LIMIT);
                // If your Order uses @GeneratedValue and refuses manual IDs, comment this out:
                if (id != null) o.setId(id);
                o.setCreatedAt(LocalDateTime.now());
                return o;
            }).toList();

            orderRepository.saveAll(restingOrders);

            // flush is OK now because we are inside a tx
            entityManager.flush();
            return null;
        });

        entityManager.clear();
    }

    @When("a new BUY order arrives:")
    public void buyOrderArrives(DataTable table) {
        Map<String, String> r = table.asMaps(String.class, String.class).get(0);

        incomingOrder = new Order();

        Long id = parseLongOrNull(r.get("id"));
        // If @GeneratedValue disallows this, remove it:
        if (id != null) incomingOrder.setId(id);

        incomingOrder.setSide(OrderSide.valueOf(req(r, "side")));
        long qty = Long.parseLong(req(r, "quantity"));
        incomingOrder.setQuantity(qty);
        incomingOrder.setRemainingQuantity(qty);

        incomingOrder.setPrice(new BigDecimal(req(r, "price")));
        incomingOrder.setInstrument(req(r, "instrument"));
        incomingOrder.setUserId(Long.parseLong(req(r, "userId")));
        incomingOrder.setStatus(OrderStatus.valueOf(req(r, "status")));

        incomingOrder.setExchangeCode(req(r, "exchangeCode"));
        incomingOrder.setType(OrderType.valueOf(req(r, "type")));
        incomingOrder.setCreatedAt(LocalDateTime.now());
        incomingOrder.setRoutingMode(RoutingMode.MANUAL);
        incomingOrder.setRoutedBy(RoutedBy.USER);
        incomingOrder.setVisible(incomingOrder.getType() != OrderType.HIDDEN_LIMIT);
        Order captured = incomingOrder;
        transactionTemplate.execute(status -> {
            incomingOrder = orderRepository.save(incomingOrder);
            matchingService.match(incomingOrder);
            return null;
        });

        entityManager.clear();
    }

    @Then("the BUY order should be {string} with {int} remaining quantity")
    public void verifyIncomingOrder(String expectedStatus, int expectedRemaining) {
        Order buy = orderRepository.findById(incomingOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.valueOf(expectedStatus), buy.getStatus());
        assertEquals(expectedRemaining, buy.getRemainingQuantity());
    }

    @Then("the SELL order {int} should be {string} with {int} remaining quantity")
    public void verifyRestingOrder(int sellOrderId, String expectedStatus, int expectedRemaining) {
        // Reload from DB to ensure we validate the persisted result of matching
        Order sell = orderRepository.findById((long) sellOrderId)
                .orElseThrow(() -> new AssertionError("SELL order not found: " + sellOrderId));

        assertEquals(OrderStatus.valueOf(expectedStatus), sell.getStatus());
        assertEquals((long) expectedRemaining, sell.getRemainingQuantity().longValue());
    }

    @Then("a trade event should be published for {int} shares at {double}")
    public void verifyKafka(int qty, double price) {
        /**
         * This verifies the SIDE EFFECT:
         * MatchingService should publish an event when a match happens.
         *
         * Because TradeEventsProducer is mocked (via @MockitoBean in config),
         * this verify does not send Kafka; it only checks that publish(...) was called
         * with correct event data.
         */
        verify(tradeEventsProducer, times(1)).publish(
                anyString(),// key can be any (often order id or trade id)
                argThat(event ->
                        event != null
                                && event.quantity() == qty
                                && event.price().compareTo(BigDecimal.valueOf(price)) == 0
                )
        );
    }

    private static String req(Map<String, String> row, String key) {
        String v = row.get(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required column: " + key);
        }
        return v.trim();
    }

    private static Long parseLongOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return Long.parseLong(t);
    }
}
