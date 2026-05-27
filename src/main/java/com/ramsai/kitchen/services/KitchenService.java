package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.ItemStatus;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.repositories.OrderItemRepository;
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
    private final InventoryService inventoryService;
    private final com.ramsai.kitchen.mappers.OrderItemMapper orderItemMapper;

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getKdsItems(ItemStatus status) {
        return orderItemRepository.findAllWithProductByItemStatusOrderByOrderCreatedAtAsc(status)
                .stream()
                .map(orderItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateItemStatus(Long itemId, ItemStatus newStatus) {
        log.info("Attempting to update status for Item {} to {}", itemId, newStatus);
        OrderItem item = orderItemRepository.findByIdWithProduct(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + itemId));

        ItemStatus oldStatus = item.getItemStatus();
        item.setItemStatus(newStatus);
        orderItemRepository.save(item);

        log.info("Successfully updated Item {} status from {} to {}", itemId, oldStatus, newStatus);

        if (newStatus == ItemStatus.COOKING && oldStatus != ItemStatus.COOKING) {
            log.info("Item {} moved to COOKING. Triggering inventory deduction.", itemId);
            inventoryService.deductStockForProduct(item.getProduct(), item.getQuantity());
        }
    }
}
