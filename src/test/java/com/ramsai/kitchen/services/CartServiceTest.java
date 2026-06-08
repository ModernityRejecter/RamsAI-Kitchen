package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.mappers.OrderItemMapper;
import com.ramsai.kitchen.mappers.OrderMapper;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RestaurantTableRepository tableRepository;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private CartService cartService;

    @Test
    void checkout_DeductsInventoryBeforeSavingOrder() {
        Long customerId = 1L;
        Long tableId = 2L;
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.DRAFT);
        order.setItems(new ArrayList<>());
        order.getItems().add(new com.ramsai.kitchen.models.entities.OrderItem());

        RestaurantTable table = new RestaurantTable();
        table.setId(tableId);

        when(orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.DRAFT)).thenReturn(Optional.of(order));
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));

        cartService.checkout(customerId, tableId, "No peanuts");

        verify(inventoryService).validateAndDeductForOrder(order);
        verify(orderRepository).save(order);
    }

    @Test
    void checkout_WhenInventoryDeductionFailsOrderNotSaved() {
        Long customerId = 1L;
        Long tableId = 2L;
        Order order = new Order();
        order.setStatus(OrderStatus.DRAFT);
        order.setItems(new ArrayList<>());
        order.getItems().add(new com.ramsai.kitchen.models.entities.OrderItem());

        RestaurantTable table = new RestaurantTable();
        table.setId(tableId);

        when(orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.DRAFT)).thenReturn(Optional.of(order));
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));
        doThrow(new RuntimeException("Insufficient stock")).when(inventoryService).validateAndDeductForOrder(order);

        assertThrows(RuntimeException.class, () -> cartService.checkout(customerId, tableId, null));
        verify(orderRepository, never()).save(order);
    }
}
