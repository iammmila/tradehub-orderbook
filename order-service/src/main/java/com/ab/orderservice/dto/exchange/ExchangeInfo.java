package com.ab.orderservice.dto.exchange;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExchangeInfo {
    private String exchangeCode; // e.g. XLON, XNAS
    private String region;       // EMEA/APAC/AMER
    private String feeModel;
    private int makerFeeBps;
    private int takerFeeBps;
}
