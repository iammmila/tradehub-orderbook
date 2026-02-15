package com.ab.orderservice.orders.config;

import com.ab.orderservice.orders.model.*;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.repository.OrderRepository;
import com.ab.orderservice.orders.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
//    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) {

        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping...");
            return;
        }

        log.info("Seeding database...");

        User user1 = User.builder()
                .email("alice@tradehub.com")
                .firstName("Alice")
                .lastName("Brown")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .email("bob@tradehub.com")
                .firstName("Bob")
                .lastName("Smith")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.saveAll(List.of(user1, user2));

        List<String> instruments = List.of("BT", "VOD", "AAPL", "TSLA");

        for (int i = 0; i < 30; i++) {

            User randomUser = random.nextBoolean() ? user1 : user2;
            OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;

            BigDecimal price = BigDecimal.valueOf(
                    90 + random.nextDouble() * 20
            ).setScale(2, RoundingMode.HALF_UP);

            long quantity = 10 + random.nextInt(100);

            Order order = Order.builder()
                    .instrument(instruments.get(random.nextInt(instruments.size())))
                    .side(side)
                    .price(price)
                    .quantity(quantity)
                    .remainingQuantity(quantity)
                    .status(OrderStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .user(randomUser)
                    .build();

            orderRepository.save(order);
        }

        log.info("Database seeding completed successfully.");
    }
}
