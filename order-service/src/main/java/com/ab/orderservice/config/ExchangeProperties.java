package com.ab.orderservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ExchangeProperties {

    private Exchange exchange = new Exchange();

    private List<String> exchanges = new ArrayList<>();

    @Data
    public static class Exchange {
        private String defaultCode = "XLON";
    }
}