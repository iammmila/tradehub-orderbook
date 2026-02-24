package com.ab.tradeservice.api;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
// loads controller + mvc + jackson + validation only (fast).
@AutoConfigureMockMvc(addFilters = false)
// addFilters=false: avoids Spring Security filters interfering.
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TradeService tradeService;

    //helper
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

    // POST /api/v1/trades
    @Test
    void createTrade_shouldReturn204_andCallService() throws Exception {
        //  Controller returns 204 No Content
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

        // Verify service received same request values
        ArgumentCaptor<CreateTradeRequest> captor = ArgumentCaptor.forClass(CreateTradeRequest.class);
        verify(tradeService).createTrade(captor.capture());
        assertThat(captor.getValue().getInstrument()).isEqualTo("AAPL");
    }

    // GET /api/v1/trades/my
    @Test
    void getMyTrades_shouldReturn403_whenNotUserOrAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/trades/my")
                        .header("X-User-Id", "10"))
                .andExpect(status().isForbidden());

        verify(tradeService, never()).getMyTradesPaged(anyLong(), any(), any());
    }

    @Test
    void getMyTrades_shouldReturn200_whenRoleUser() throws Exception {
        Page<TradeResponse> page = new PageImpl<>(
                List.of(sample(1L)),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(tradeService.getMyTradesPaged(eq(10L), eq("BT"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/trades/my")
                        .header("X-User-Id", "10")
                        .header("X-Roles", "ROLE_USER")
                        .param("instrument", "BT")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(tradeService).getMyTradesPaged(eq(10L), eq("BT"), any(Pageable.class));
    }

    // GET /api/v1/trades  (ADMIN only)
    @Test
    void getTrades_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());

        verify(tradeService, never()).getTrades(any());
    }

    @Test
    void getTrades_shouldReturn200_whenAdmin_andCallService() throws Exception {
        when(tradeService.getTrades(eq("AAPL"))).thenReturn(List.of(sample(1L)));

        mockMvc.perform(get("/api/v1/trades")
                        .header("X-Roles", "ROLE_ADMIN")
                        .param("instrument", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(tradeService).getTrades("AAPL");
    }
}