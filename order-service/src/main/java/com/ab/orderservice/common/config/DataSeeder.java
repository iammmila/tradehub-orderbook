package com.ab.orderservice.common.config;

import com.ab.orderservice.auth.model.Role;
import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.auth.repository.RoleRepository;
import com.ab.orderservice.orders.model.*;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.repository.OrderRepository;
import com.ab.orderservice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    //    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        seedRoles();
        User admin = seedAdminUser();
        User user = seedNormalUser();

        seedOrdersIfEmpty(admin, user);
    }

    private void seedRoles() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    log.info("Seeding role: ROLE_USER");
                    return roleRepository.save(Role.builder().name("ROLE_USER").build());
                });

        roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    log.info("Seeding role: ROLE_ADMIN");
                    return roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
                });
    }

    private User seedAdminUser() {
        return userRepository.findByUsername("admin")
                .orElseGet(() -> {
                    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                            .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found (seeding failed)"));

                    log.info("Seeding admin user: admin");

                    User admin = User.builder()
                            .username("admin")
                            .firstName("admin")
                            .lastName("admin")
                            .email("admin@tradehub.com")
                            .password(passwordEncoder.encode("admin123"))
                            .role(adminRole)
                            .build();

                    return userRepository.save(admin);
                });
    }

    private User seedNormalUser() {
        return userRepository.findByUsername("user")
                .orElseGet(() -> {
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new IllegalStateException("ROLE_USER not found (seeding failed)"));

                    log.info("Seeding normal user: user");

                    User user = User.builder()
                            .username("user")
                            .firstName("firstname")
                            .lastName("lastname")
                            .email("user@tradehub.com")
                            .password(passwordEncoder.encode("user123"))
                            .role(userRole)
                            .build();

                    return userRepository.save(user);
                });
    }

    private void seedOrdersIfEmpty(User admin, User user) {
        if (orderRepository.count() > 0) {
            log.info("Orders already seeded. Skipping...");
            return;
        }

        log.info("Seeding demo orders...");

        List<String> instruments = List.of("BT", "VOD", "AAPL", "TSLA");

        for (int i = 0; i < 30; i++) {
            User randomUser = random.nextBoolean() ? admin : user;
            OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;

            BigDecimal price = BigDecimal.valueOf(90 + random.nextDouble() * 20)
                    .setScale(2, RoundingMode.HALF_UP);

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

        log.info("Order seeding completed successfully.");
    }
}
