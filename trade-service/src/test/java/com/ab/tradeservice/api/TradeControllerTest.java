package com.ab.tradeservice.api;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.security.AuthPrincipal;
import com.ab.tradeservice.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
@Import(TradeControllerTest.TestSecurityConfig.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TradeService tradeService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .httpBasic(Customizer.withDefaults())
                    .authorizeHttpRequests(auth -> auth
                            // Called by order-service via Feign.
                            // In many systems this is protected by service-to-service auth.
                            // For controller slice tests we allow it to focus on controller behavior.
                            .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/trades").permitAll()

                            // /my must be authenticated (ROLE_USER or ROLE_ADMIN are both fine)
                            .requestMatchers("/api/v1/trades/my").authenticated()

                            // /api/v1/trades (list all) admin only
                            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/trades").hasRole("ADMIN")

                            .anyRequest().denyAll()
                    )
                    .build();
        }
    }

    // helper
    private TradeResponse sample(Long id) {
        return TradeResponse.builder()
                .id(id)
                .instrument("AAPL")
                .price(new BigDecimal("150.25"))
                .quantity(10L)
                .buyOrderId(1L)
                .sellOrderId(2L)
                .buyerUserId(10L)
                .sellerUserId(20L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UsernamePasswordAuthenticationToken auth(long userId, String username, String... roles) {
        // roles should be like "ROLE_USER", "ROLE_ADMIN"
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();

        var principal = new AuthPrincipal(username, userId);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    // POST /api/v1/trades
    @Test
    void createTrade_shouldReturn204_andCallService() throws Exception {
        CreateTradeRequest req = CreateTradeRequest.builder()
                .instrument("AAPL")
                .price(new BigDecimal("150.25"))
                .quantity(10L)
                .buyOrderId(1L)
                .sellOrderId(2L)
                .buyerUserId(10L)
                .sellerUserId(20L)
                .createdAt(LocalDateTime.of(2026, 2, 24, 10, 0))
                .build();

        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<CreateTradeRequest> captor = ArgumentCaptor.forClass(CreateTradeRequest.class);
        verify(tradeService).createTrade(captor.capture());
        assertThat(captor.getValue().getInstrument()).isEqualTo("AAPL");
        assertThat(captor.getValue().getBuyerUserId()).isEqualTo(10L);
    }

    // GET /api/v1/trades/my  (requires authentication)
    @Test
    void getMyTrades_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/trades/my"))
                .andExpect(status().isUnauthorized());

        verify(tradeService, never()).getMyTradesPaged(anyLong(), any(), any());
    }

    @Test
    void getMyTrades_shouldReturn200_whenAuthenticatedUser() throws Exception {
        Page<TradeResponse> page = new PageImpl<>(
                List.of(sample(1L)),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(tradeService.getMyTradesPaged(eq(10L), eq("BT"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/trades/my")
                        .with(authentication(auth(10L, "malahat", "ROLE_USER")))
                        .param("instrument", "BT")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].instrument").value("AAPL"));

        verify(tradeService).getMyTradesPaged(eq(10L), eq("BT"), any(Pageable.class));
    }

    // GET /api/v1/trades  (ADMIN only in our test security config)
    @Test
    void getTrades_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/trades"))
                .andExpect(status().isUnauthorized());

        verify(tradeService, never()).getTrades(any());
    }

    @Test
    void getTrades_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .with(authentication(auth(10L, "malahat", "ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(tradeService, never()).getTrades(any());
    }

    @Test
    void getTrades_shouldReturn200_whenAdmin_andCallService() throws Exception {
        when(tradeService.getTrades(eq("AAPL"))).thenReturn(List.of(sample(1L)));

        mockMvc.perform(get("/api/v1/trades")
                        .with(authentication(auth(1L, "admin", "ROLE_ADMIN")))
                        .param("instrument", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(tradeService).getTrades("AAPL");
    }
}