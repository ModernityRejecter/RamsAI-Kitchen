package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.ItemStatus;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KitchenService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getKdsItems(ItemStatus status) {
        return orderItemRepository.findAllByItemStatusOrderByOrderCreatedAtAsc(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateItemStatus(Long itemId, ItemStatus newStatus) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found"));

        ItemStatus oldStatus = item.getItemStatus();
        item.setItemStatus(newStatus);
        orderItemRepository.save(item);

        log.info("Updated Item {} status from {} to {}", itemId, oldStatus, newStatus);

        if (newStatus == ItemStatus.COOKING && oldStatus != ItemStatus.COOKING) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            inventoryService.deductStockForProduct(product, item.getQuantity());
        }
    }

    private OrderItemResponse mapToResponse(OrderItem item) {
        Product product = productRepository.findById(item.getProductId()).orElse(null);
        String productName = (product != null) ? product.getName() : "Unknown Product";
        
        return new OrderItemResponse(
                item.getId(),
                item.getOrder().getId(),
                item.getProductId(),
                productName,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSpecialNotes(),
                item.getItemStatus()
        );
    }
}
