package com.ab.orderservice.config;

import com.ab.orderservice.model.enums.*;
import com.ab.orderservice.service.ExchangeRegistry;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("dev") // only run seeding in dev
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final ExchangeRegistry exchangeRegistry;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        seedOrdersIfEmpty();
    }

    private void seedOrdersIfEmpty() {
        if (orderRepository.count() > 0) {
            log.info("Orders already exist. Skipping OrderDataSeeder.");
            return;
        }

        log.info("Seeding demo orders...");

        List<Long> demoUserIds = List.of(1L, 2L);

        List<String> instruments = List.of("BT", "VOD", "AAPL", "TSLA");

        List<OrderType> types = List.of(
                OrderType.LIMIT,
                OrderType.LIMIT,
                OrderType.LIMIT,
                OrderType.HIDDEN_LIMIT
        );
        for (int i = 0; i < 30; i++) {
            Long userId = demoUserIds.get(random.nextInt(demoUserIds.size()));
            OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;

            OrderType type = types.get(random.nextInt(types.size()));
            boolean visible = type != OrderType.HIDDEN_LIMIT;

            BigDecimal price = BigDecimal.valueOf(90 + random.nextDouble() * 20)
                    .setScale(2, RoundingMode.HALF_UP);

            long qty = 10 + random.nextInt(100);

            RoutingMode routingMode = random.nextBoolean() ? RoutingMode.MANUAL : RoutingMode.AUTO;
            RoutedBy routedBy = (routingMode == RoutingMode.MANUAL) ? RoutedBy.USER : RoutedBy.SOR;
            String routeReason = (routingMode == RoutingMode.AUTO)
                    ? "DEV_SEED_AUTO"
                    : null;

            Order order = Order.builder()
                    .instrument(instruments.get(random.nextInt(instruments.size())))
                    .side(side)
                    .exchangeCode(pickExchange())
                    .type(type)
                    .visible(visible)
                    .minExecSize(null)
                    .price(price)
                    .quantity(qty)
                    .remainingQuantity(qty)
                    .status(OrderStatus.NEW)
                    .createdAt(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                    .routingMode(routingMode)
                    .routedBy(routedBy)
                    .routeReason(routeReason)
                    .userId(userId)
                    .build();

            orderRepository.save(order);
        }

        log.info("Order seeding completed.");
    }

    private String pickExchange() {
        var codes = exchangeRegistry.codes().stream().toList();
        return codes.get(random.nextInt(codes.size()));
    }
}