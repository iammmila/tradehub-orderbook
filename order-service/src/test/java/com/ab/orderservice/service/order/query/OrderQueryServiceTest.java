package com.ab.orderservice.service.order.query;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.order.support.OrderAccessChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAccessChecker accessChecker;

    @InjectMocks
    private OrderQueryService service;

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L, 10L, false))
                .isInstanceOf(NotFoundException.class);

        verify(accessChecker, never()).requireOwnerOrAdmin(any(), anyLong(), anyBoolean());
    }

    @Test
    void getById_shouldRequireAccess_andReturnResponse() {
        Order order = Order.builder()
                .id(5L)
                .userId(10L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .status(OrderStatus.NEW)
                .price(new BigDecimal("1.00"))
                .quantity(10L)
                .remainingQuantity(10L)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);

        OrderResponse resp = service.getById(5L, 10L, false);

        verify(accessChecker).requireOwnerOrAdmin(order, 10L, false);
        assertThat(resp.getId()).isEqualTo(5L);
        assertThat(resp.getInstrument()).isEqualTo("AAPL");
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void list_shouldFilterAndMap_toResponses() {
        Order o1 = Order.builder().id(1L).instrument("AAPL").side(OrderSide.BUY).status(OrderStatus.NEW).build();
        Order o2 = Order.builder().id(2L).instrument("AAPL").side(OrderSide.BUY).status(OrderStatus.NEW).build();

        // Specification is built inside service; here we just return data for any spec.
        when(orderRepository.findAll(Mockito.<Specification<Order>>any()))
                .thenReturn(List.of(o1, o2));

        List<OrderResponse> resp = service.list(OrderSide.BUY, "AAPL", OrderStatus.NEW);

        assertThat(resp).hasSize(2);
        assertThat(resp.get(0).getId()).isEqualTo(1L);
        assertThat(resp.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void listByUserPaged_shouldReturnPageMapped() {
        Order o1 = Order.builder().id(10L).userId(7L).instrument("AAPL").build();
        Order o2 = Order.builder().id(11L).userId(7L).instrument("AAPL").build();

        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
        Page<Order> page = new PageImpl<>(List.of(o1, o2), pageable, 2);

        when(orderRepository.findAll(Mockito.<Specification<Order>>any(), eq(pageable)))
                .thenReturn(page);

        Page<OrderResponse> resp = service.listByUserPaged(7L, null, null, null, pageable);

        assertThat(resp.getTotalElements()).isEqualTo(2);
        assertThat(resp.getContent()).hasSize(2);
        assertThat(resp.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(resp.getContent().get(1).getId()).isEqualTo(11L);
    }
}
