package com.ab.orderservice.model;

import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderType type; // LIMIT / MARKET / etc.

    // For MIN_EXECUTION_SIZE
    @Column(name = "min_exec_size")
    private Long minExecSize;

    // Whether it should appear in public orderbook (HIDDEN_LIMIT => false)
    @Column(nullable = false)
    private Boolean visible;

    @Column(name = "exchange_code", nullable = false, length = 16)
    private String exchangeCode;

    @PrePersist
    void prePersist() {
        // normalize if set somewhere else
        if (exchangeCode != null) {
            exchangeCode = exchangeCode.trim().toUpperCase(Locale.ROOT);
        }
    }

    // e.g. "BT", "VOD", "AAPL"
    @Column(nullable = false, length = 20)
    private String instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
