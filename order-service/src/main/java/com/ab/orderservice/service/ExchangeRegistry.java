package com.ab.orderservice.service;

import com.ab.orderservice.config.ExchangeProperties;
import com.ab.orderservice.dto.exchange.ExchangeInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExchangeRegistry {

    private final ExchangeProperties props;

    private final Map<String, ExchangeInfo> map = new LinkedHashMap<>();

    public ExchangeInfo info(String exchangeCode) {
        return map.get(normalize(exchangeCode));
    }

    @PostConstruct
    void init() {
        List<String> codes = props.getExchanges();
        if (codes == null || codes.isEmpty()) {
            codes = List.of(props.getExchange().getDefaultCode());
        }

        for (String c : codes) {
            String code = normalize(c);
            int maker = switch (code) {
                case "XLON" -> 1;
                case "XNAS" -> 0;
                case "XTKS" -> 2;
                default -> 1;
            };
            int taker = switch (code) {
                case "XLON" -> 2;
                case "XNAS" -> 3;
                case "XTKS" -> 4;
                default -> 2;
            };
            map.put(code, new ExchangeInfo(code, guessRegion(code), "MAKER_TAKER", maker, taker));
        }
    }

    public String defaultCode() {
        return normalize(props.getExchange().getDefaultCode());
    }

    public boolean isSupported(String code) {
        if (code == null) return false;
        return map.containsKey(normalize(code));
    }

    public String normalizeOrDefault(String code) {
        String norm = normalize(code);
        if (norm == null || norm.isBlank()) return defaultCode();
        return isSupported(norm) ? norm : defaultCode();
    }

    public List<ExchangeInfo> list() {
        return new ArrayList<>(map.values());
    }

    public Set<String> codes() {
        return map.
                keySet()
                .stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalize(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String guessRegion(String code) {
        return switch (code) {
            case "XLON" -> "EMEA";
            case "XNAS" -> "AMER";
            case "XTKS" -> "APAC";
            default -> "EMEA";
        };
    }
}
