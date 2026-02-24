package com.ab.tradeservice.service;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.model.Trade;
import com.ab.tradeservice.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//pure unit tests, no Spring context -> fast and stable.
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;
    // Mockito creates TradeService and injects mocked repository.

    @Test
    void createTrade_shouldUseProvidedCreatedAt_whenNotNull() {
        //  createTrade should not override createdAt if request already includes it.
        LocalDateTime provided = LocalDateTime.of(2026, 2, 24, 10, 30);

        CreateTradeRequest req = CreateTradeRequest.builder()
                .instrument("AAPL")
                .price(new BigDecimal("150.25"))
                .quantity(10L)
                .buyOrderId(1L)
                .sellOrderId(2L)
                .buyerUserId(10L)
                .sellerUserId(20L)
                .createdAt(provided)
                .build();

        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));

        tradeService.createTrade(req);

        //  ArgumentCaptor: validate what was persisted.
        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(captor.capture());

        Trade saved = captor.getValue();
        assertThat(saved.getInstrument()).isEqualTo("AAPL");
        assertThat(saved.getPrice()).isEqualByComparingTo("150.25");
        assertThat(saved.getQuantity()).isEqualTo(10L);
        assertThat(saved.getBuyOrderId()).isEqualTo(1L);
        assertThat(saved.getSellOrderId()).isEqualTo(2L);
        assertThat(saved.getBuyerUserId()).isEqualTo(10L);
        assertThat(saved.getSellerUserId()).isEqualTo(20L);

        // Key assertion: createdAt preserved
        assertThat(saved.getCreatedAt()).isEqualTo(provided);
    }

    @Test
    void createTrade_shouldSetCreatedAt_whenNull() {
        //  request may come without createdAt; service must set it to "now".
        CreateTradeRequest req = CreateTradeRequest.builder()
                .instrument("AAPL")
                .price(new BigDecimal("150.25"))
                .quantity(10L)
                .buyOrderId(1L)
                .sellOrderId(2L)
                .buyerUserId(10L)
                .sellerUserId(20L)
                .createdAt(null)
                .build();

        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));

        tradeService.createTrade(req);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(captor.capture());

        Trade saved = captor.getValue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void getTrades_shouldReturnAllTrades_whenInstrumentNullOrBlank() {
        // sservice normalizes instrument; null/blank -> return all trades.

        Trade t1 = Trade.builder().id(1L).instrument("AAPL").price(new BigDecimal("1.00")).quantity(1L).createdAt(LocalDateTime.now()).build();
        Trade t2 = Trade.builder().id(2L).instrument("VOD").price(new BigDecimal("2.00")).quantity(2L).createdAt(LocalDateTime.now()).build();

        when(tradeRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TradeResponse> resp1 = tradeService.getTrades(null);
        List<TradeResponse> resp2 = tradeService.getTrades("   "); // blank

        assertThat(resp1).hasSize(2);
        assertThat(resp2).hasSize(2);

        // Ensure findByInstrument was never used for blank/null
        verify(tradeRepository, times(2)).findAll();
        verify(tradeRepository, never()).findByInstrument(anyString());
    }

    @Test
    void getTrades_shouldTrimInstrument_andUseFindByInstrument() {
        // service trims instrument before querying repository.
        Trade t = Trade.builder().id(1L).instrument("BT").price(new BigDecimal("3.00")).quantity(3L).createdAt(LocalDateTime.now()).build();
        when(tradeRepository.findByInstrument("BT")).thenReturn(List.of(t));

        List<TradeResponse> resp = tradeService.getTrades("  BT  ");

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getInstrument()).isEqualTo("BT");

        verify(tradeRepository).findByInstrument("BT");
        verify(tradeRepository, never()).findAll();
    }

    @Test
    void getMyTradesPaged_shouldDelegateToRepositoryFindAllWithSpec_andMapResponses() {
        // service should call repository.findAll(spec, pageable) and map to TradeResponse.
        Trade t = Trade.builder()
                .id(10L)
                .instrument("AAPL")
                .price(new BigDecimal("150.25"))
                .quantity(10L)
                .buyOrderId(1L)
                .sellOrderId(2L)
                .buyerUserId(10L)
                .sellerUserId(20L)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Trade> tradePage = new PageImpl<>(
                List.of(t),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(tradeRepository.findAll(
                ArgumentMatchers.<Specification<Trade>>any(),
                any(Pageable.class)
        )).thenReturn(tradePage);

        Page<TradeResponse> result = tradeService.getMyTradesPaged(
                10L,
                "AAPL",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getInstrument()).isEqualTo("AAPL");

        verify(tradeRepository).findAll(
                ArgumentMatchers.<Specification<Trade>>any(),
                any(Pageable.class)
        );
    }
}
