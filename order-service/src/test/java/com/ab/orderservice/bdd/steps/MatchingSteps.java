package com.ab.orderservice.bdd.steps;

import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.*;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.MatchingService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
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

    @Before
    public void resetMocks() {
        // Helps if add multiple scenarios later
        reset(tradeEventsProducer);
    }

    @Given("the following resting orders exist in the book:")
    public void existingOrders(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        transactionTemplate.execute(tx -> {
            List<Order> restingOrders = rows.stream().map(r -> {
                Order o = new Order();

                Long id = parseLongOrNull(r.get("id"));
                if (id != null) o.setId(id); // remove if @GeneratedValue disallows it

                o.setSide(OrderSide.valueOf(req(r, "side")));

                long qty = Long.parseLong(req(r, "quantity"));
                o.setQuantity(qty);
                o.setRemainingQuantity(qty);

                o.setPrice(new BigDecimal(req(r, "price")));
                o.setInstrument(req(r, "instrument"));
                o.setUserId(Long.parseLong(req(r, "userId")));
                o.setStatus(OrderStatus.valueOf(req(r, "status")));

                o.setExchangeCode(req(r, "exchangeCode"));
                o.setType(OrderType.valueOf(req(r, "type")));
                o.setVisible(o.getType() != OrderType.HIDDEN_LIMIT);

                o.setRoutingMode(RoutingMode.MANUAL);
                o.setRoutedBy(RoutedBy.USER);
                o.setCreatedAt(LocalDateTime.now());

                return o;
            }).toList();

            orderRepository.saveAll(restingOrders);
            entityManager.flush();
            return null;
        });

        entityManager.clear();
    }

    @When("a new BUY order arrives:")
    public void buyOrderArrives(DataTable table) {
        Map<String, String> r = table.asMaps(String.class, String.class).get(0);

        Order o = new Order();

        Long id = parseLongOrNull(r.get("id"));
        if (id != null) o.setId(id); // remove if @GeneratedValue disallows it

        o.setSide(OrderSide.valueOf(req(r, "side")));

        long qty = Long.parseLong(req(r, "quantity"));
        o.setQuantity(qty);
        o.setRemainingQuantity(qty);

        o.setPrice(new BigDecimal(req(r, "price")));
        o.setInstrument(req(r, "instrument"));
        o.setUserId(Long.parseLong(req(r, "userId")));
        o.setStatus(OrderStatus.valueOf(req(r, "status")));

        o.setExchangeCode(req(r, "exchangeCode"));
        o.setType(OrderType.valueOf(req(r, "type")));
        o.setVisible(o.getType() != OrderType.HIDDEN_LIMIT);

        o.setRoutingMode(RoutingMode.MANUAL);
        o.setRoutedBy(RoutedBy.USER);
        o.setCreatedAt(LocalDateTime.now());

        transactionTemplate.execute(tx -> {
            incomingOrder = orderRepository.save(o);
            matchingService.match(incomingOrder);
            entityManager.flush();
            return null;
        });

        entityManager.clear();
    }

    @Then("the BUY order should be {string} with {int} remaining quantity")
    public void verifyIncomingOrder(String expectedStatus, int expectedRemaining) {
        Order buy = orderRepository.findById(incomingOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.valueOf(expectedStatus), buy.getStatus());
        assertEquals((long) expectedRemaining, buy.getRemainingQuantity());
    }

    @Then("the SELL order {int} should be {string} with {int} remaining quantity")
    public void verifyRestingOrder(int sellOrderId, String expectedStatus, int expectedRemaining) {
        // Reload from DB to ensure we validate the persisted result of matching
        Order sell = orderRepository.findById((long) sellOrderId)
                .orElseThrow(() -> new AssertionError("SELL order not found: " + sellOrderId));

        assertEquals(OrderStatus.valueOf(expectedStatus), sell.getStatus());
        assertEquals((long) expectedRemaining, sell.getRemainingQuantity());
    }

    @Then("a trade event should be published for {int} shares at {double}")
    public void verifyKafka(int qty, double price) {
        // In MatchingEventPublisher:
        // tradeEventsProducer.publish(String.valueOf(buy.getId()), event)
        String expectedKey = String.valueOf(incomingOrder.getId()); // scenario BUY is incoming

        verify(tradeEventsProducer, times(1)).publish(
                eq(expectedKey),
                argThat((TradeCreatedEvent event) ->
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
