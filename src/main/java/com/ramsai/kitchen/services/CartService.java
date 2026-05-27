package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.enums.ItemStatus;
import com.ramsai.kitchen.mappers.OrderItemMapper;
import com.ramsai.kitchen.models.dtos.CartItemRequest;
import com.ramsai.kitchen.models.dtos.CartResponse;
import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;

    @Transactional
    public CartResponse getCart(Long customerId) {
        Order cart = getOrCreateDraftOrder(customerId);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long customerId, CartItemRequest request) {
        Order cart = getOrCreateDraftOrder(customerId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        // Check if item already exists
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                    existingItem -> existingItem.setQuantity(existingItem.getQuantity() + request.quantity()),
                    () -> {
                        OrderItem newItem = new OrderItem();
                        newItem.setOrder(cart);
                        newItem.setProduct(product);
                        newItem.setQuantity(request.quantity());
                        newItem.setUnitPrice(product.isSpecialOffer() ? product.getDiscountPrice() : product.getBasePrice());
                        newItem.setSpecialNotes(request.specialNotes());
                        newItem.setItemStatus(ItemStatus.PENDING);
                        cart.getItems().add(newItem);
                    }
                );
        
        recalculateTotal(cart);
        Order savedCart = orderRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItem(Long customerId, Long itemId) {
        Order cart = getOrCreateDraftOrder(customerId);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        
        recalculateTotal(cart);
        Order savedCart = orderRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Transactional
    public void checkout(Long customerId, Long tableId) {
        Order cart = orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.DRAFT)
                .orElseThrow(() -> new RuntimeException("No active cart found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout an empty cart");
        }

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Reserve stock at order reception time (checkout).
        inventoryService.validateAndDeductForOrder(cart);

        List<RestaurantTable> group = tableRepository.findAll().stream()
                .filter(t -> t.getTableNumber().equals(table.getTableNumber()))
                .collect(Collectors.toList());

        group.forEach(t -> {
            t.setStatus(com.ramsai.kitchen.enums.TableStatus.OCCUPIED);
            t.setOccupiedByUserId(customerId);
        });
        tableRepository.saveAll(group);

        cart.setTable(table);
        cart.setStatus(OrderStatus.RECEIVED);
        orderRepository.save(cart);
        log.info("Order {} checked out for table {}", cart.getId(), tableId);
    }

    private Order getOrCreateDraftOrder(Long customerId) {
        return orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.DRAFT)
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setCustomerId(customerId);
                    newOrder.setStatus(OrderStatus.DRAFT);
                    newOrder.setItems(new ArrayList<>());
                    return orderRepository.save(newOrder);
                });
    }

    private void recalculateTotal(Order order) {
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPrice(total);
    }

    private CartResponse mapToCartResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(orderItemMapper::toResponse)
                .collect(Collectors.toList());
        return new CartResponse(order.getId(), itemResponses, order.getTotalPrice());
    }
}
