package com.ab.orderservice.api;

import com.ab.orderservice.dto.CreateOrderRequest;
import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.security.AuthPrincipal;
import com.ab.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest(OrderController.class) // loads only web layer
// It does NOT start DB, Kafka, full Spring context -> fast & focused controller tests.
@Import(OrderControllerTest.TestSecurityConfig.class)
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;
    //MockMvc: performs HTTP-like requests in-memory without starting a real server.

    @Autowired
    private ObjectMapper objectMapper;
    // ObjectMapper: easiest way to serialize DTOs to JSON (same as runtime).

    @MockitoBean
    private OrderService orderService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .httpBasic(Customizer.withDefaults())
                    .authorizeHttpRequests(auth -> auth
                            // In controller, all endpoints require @AuthenticationPrincipal me.
                            // So we require authentication for everything under /api/v1/orders/**.
                            .requestMatchers("/api/v1/orders/**").authenticated()
                            .anyRequest().denyAll()
                    )
                    .build();
        }
    }

    // Helper: sample response
    private OrderResponse sampleResponse(Long id) {
        return OrderResponse.builder()
                .id(id)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("150.25"))
                .quantity(100L)
                .remainingQuantity(100L)
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .userId(10L)
                .build();
    }

    /**
     * Creates an Authentication that mimics what JWT normally gives you:
     * - principal = AuthPrincipal(username, userId)
     * - authorities = ROLE_USER / ROLE_ADMIN etc
     * <p>
     * Note: your controller currently does NOT use roles (it hardcodes isAdmin=false),
     * but we keep roles here because you will likely add admin logic later.
     */
    private UsernamePasswordAuthenticationToken auth(long userId, String username, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var principal = new AuthPrincipal(username, userId);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    // GET /api/v1/orders
    @Test
    void getOrders_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).getOrders(any(), any(), any());
    }

    @Test
    void getOrders_shouldReturn200_andCallServiceWithFilters() throws Exception {
        when(orderService.getOrders(any(), any(), any()))
                .thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/v1/orders")
                        .with(authentication(auth(10L, "user", "ROLE_USER")))
                        .param("side", "BUY")
                        .param("instrument", "AAPL")
                        .param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(orderService).getOrders(OrderSide.BUY, "AAPL", OrderStatus.NEW);
    }

    @Test
    void getOrders_shouldAllowMissingFilters() throws Exception {
        when(orderService.getOrders(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders")
                        .with(authentication(auth(10L, "user", "ROLE_USER"))))
                .andExpect(status().isOk());

        verify(orderService).getOrders(null, null, null);
    }

    // GET /api/v1/orders/my
    @Test
    void getOrdersByUser_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders/my"))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).getOrdersByUserPaged(anyLong(), any(), any(), any(), any());
    }

    @Test
    void getOrdersByUser_shouldReturn200_whenAuthenticated() throws Exception {
        Page<OrderResponse> page = new PageImpl<>(
                List.of(sampleResponse(1L)),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(orderService.getOrdersByUserPaged(eq(10L), eq(OrderSide.BUY), eq("AAPL"), eq(OrderStatus.NEW), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/orders/my")
                        .with(authentication(auth(10L, "user", "ROLE_USER")))
                        .param("side", "BUY")
                        .param("instrument", "AAPL")
                        .param("status", "NEW")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());

        verify(orderService).getOrdersByUserPaged(eq(10L), eq(OrderSide.BUY), eq("AAPL"), eq(OrderStatus.NEW), any(Pageable.class));
    }

    // POST /api/v1/orders
    @Test
    void createOrder_shouldReturn401_whenNotAuthenticated() throws Exception {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.25"))
                .quantity(100L)
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).createOrder(anyLong(), any());
    }

    @Test
    void createOrder_shouldReturn201_andLocationHeader_whenAuthenticated() throws Exception {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.25"))
                .quantity(100L)
                .build();

        when(orderService.createOrder(eq(10L), any(CreateOrderRequest.class)))
                .thenReturn(sampleResponse(55L));

        mockMvc.perform(post("/api/v1/orders")
                        .with(authentication(auth(10L, "user", "ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/55"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(55));

        // Verify service called with correct userId and request values
        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).createOrder(eq(10L), captor.capture());
        assertThat(captor.getValue().getInstrument()).isEqualTo("AAPL");
    }

    @Test
    void createOrder_shouldReturn400_whenValidationFails() throws Exception {
        CreateOrderRequest invalid = CreateOrderRequest.builder()
                .instrument("") // @NotBlank
                .side(null)     // @NotNull
                .price(null)    // @NotNull (for LIMIT)
                .quantity(-1L)  // @Positive
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .with(authentication(auth(10L, "user", "ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(anyLong(), any());
    }

    // DELETE /api/v1/orders/{orderId}
    @Test
    void cancelOrder_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/99"))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).cancelOrder(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void cancelOrder_shouldCallService_withIsAdminFalse() throws Exception {
        when(orderService.cancelOrder(eq(99L), eq(10L), eq(false)))
                .thenReturn(sampleResponse(99L));

        mockMvc.perform(delete("/api/v1/orders/99")
                        .with(authentication(auth(10L, "user", "ROLE_USER"))))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(99L, 10L, false);
    }
}