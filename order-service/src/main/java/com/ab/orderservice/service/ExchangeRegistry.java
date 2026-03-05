package com.ab.orderservice.service;

import com.ab.orderservice.config.ExchangeProperties;
import com.ab.orderservice.dto.exchange.ExchangeInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exchange lookup for validation, defaults, and UI dropdowns.
 * Built once at startup from application.properties.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRegistry {
    private final ExchangeProperties props;

    // In-memory registry used by services to validate and resolve exchange codes.
    private Map<String, ExchangeInfo> exchangeByCode = Map.of();

    /**
     * Returns exchange metadata by code, or null if missing.
     */
    public ExchangeInfo info(String exchangeCode) {
        return exchangeByCode.get(normalize(exchangeCode));
    }

    /**
     * Loads supported exchanges into an immutable map on application startup.
     */
    @PostConstruct
    void init() {
        List<String> codes = props.getExchanges();
        if (codes == null || codes.isEmpty()) {
            codes = List.of(props.getExchange().getDefaultCode());
        }

        Map<String, ExchangeInfo> tmp = new LinkedHashMap<>();

        for (String c : codes) {
            String code = normalize(c);
            if (code == null || code.isBlank()) continue;

            int makerBps = makerFeeBps(code);
            int takerBps = takerFeeBps(code);

            tmp.put(code, new ExchangeInfo(
                    code,
                    guessRegion(code),
                    "MAKER_TAKER",
                    makerBps,
                    takerBps
            ));
        }

        // Immutable snapshot used by all threads after startup.
        this.exchangeByCode = Collections.unmodifiableMap(tmp);
    }

    /**
     * Returns the configured default exchange code (normalized).
     */
    public String defaultCode() {
        return normalize(props.getExchange().getDefaultCode());
    }

    /**
     * Validates that a code exists in the registry.
     */
    public boolean isSupported(String code) {
        if (code == null) return false;
        return exchangeByCode.containsKey(normalize(code));
    }

    /**
     * Normalizes the input and falls back to default when blank or unsupported.
     */
    public String normalizeOrDefault(String code) {
        String norm = normalize(code);
        if (norm == null || norm.isBlank()) return defaultCode();
        return isSupported(norm) ? norm : defaultCode();
    }

    /**
     * Returns full exchange info list for UI / API output.
     */
    public List<ExchangeInfo> list() {
        return new ArrayList<>(exchangeByCode.values());
    }

    /**
     * Returns supported exchange codes preserving configuration order.
     */
    public Set<String> codes() {
        return exchangeByCode.keySet()
                .stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Canonical form for consistent comparisons and map keys.
    private static String normalize(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase(Locale.ROOT);
    }

    // Simple region classification used for display / grouping.
    private static String guessRegion(String code) {
        return switch (code) {
            case "XLON" -> "EMEA";
            case "XNAS" -> "AMER";
            case "XTKS" -> "APAC";
            default -> "EMEA";
        };
    }

    // Fee table per exchange in basis points.
    private static int makerFeeBps(String code) {
        return switch (code) {
            case "XLON" -> 1;
            case "XNAS" -> 0;
            case "XTKS" -> 2;
            default -> 1;
        };
    }

    // Fee table per exchange in basis points.
    private static int takerFeeBps(String code) {
        return switch (code) {
            case "XLON" -> 2;
            case "XNAS" -> 3;
            case "XTKS" -> 4;
            default -> 2;
        };
    }
}