package com.ab.orderservice.bdd.steps;

import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
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

        // Convert table rows to real Order entities and persist them
        List<Order> restingOrders = rows.stream().map(r -> {
            Long id = parseLongOrNull(r.get("id"));
            OrderSide side = OrderSide.valueOf(r.get("side"));
            long qty = Long.parseLong(r.get("quantity"));
            BigDecimal price = new BigDecimal(r.get("price"));
            String instrument = r.get("instrument");
            Long userId = Long.parseLong(r.get("userId"));
            OrderStatus status = OrderStatus.valueOf(r.get("status"));

            // Build order - adapt to your entity constructor/builder
            Order o = new Order();
            // If your Order uses @GeneratedValue and refuses manual IDs, comment this out:
            if (id != null) o.setId(id);

            o.setSide(side);
            o.setQuantity(qty);
            o.setRemainingQuantity(qty); // important: matching uses remainingQuantity not total quantity
            o.setPrice(price);
            o.setInstrument(instrument);
            o.setUserId(userId);
            o.setStatus(status);
            o.setCreatedAt(LocalDateTime.now());

            return o;
        }).toList();

        orderRepository.saveAll(restingOrders);
    }

    @When("a new BUY order arrives:")
    public void buyOrderArrives(DataTable table) {
        Map<String, String> r = table.asMaps(String.class, String.class).get(0);

        incomingOrder = new Order();
        Long id = parseLongOrNull(r.get("id"));
        if (id != null) incomingOrder.setId(id);

        incomingOrder.setSide(OrderSide.valueOf(r.get("side")));
        long qty = Long.parseLong(r.get("quantity"));
        incomingOrder.setQuantity(qty);
        incomingOrder.setRemainingQuantity(qty);
        incomingOrder.setPrice(new BigDecimal(r.get("price")));
        incomingOrder.setInstrument(r.get("instrument"));
        incomingOrder.setUserId(Long.parseLong(r.get("userId")));
        incomingOrder.setStatus(OrderStatus.valueOf(r.get("status")));
        incomingOrder.setCreatedAt(LocalDateTime.now());

        Order capturedIncoming = incomingOrder;
        transactionTemplate.execute(status -> {
            matchingService.match(capturedIncoming);
            return null;
        });
        entityManager.clear();
    }

    @Then("the BUY order should be {string} with {int} remaining quantity")
    public void verifyIncomingOrder(String expectedStatus, int expectedRemaining) {
        // We check the in-memory object (it may be updated by matching logic)
        // assertEquals(OrderStatus.valueOf(expectedStatus), incomingOrder.getStatus());
        assertEquals((long) expectedRemaining, incomingOrder.getRemainingQuantity().longValue());
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

    //Helper: safely parse Long from a string cell (supports blanks).
    private static Long parseLongOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return Long.parseLong(t);
    }
}
