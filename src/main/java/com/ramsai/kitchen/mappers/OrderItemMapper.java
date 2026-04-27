package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "productName", source = "productId", qualifiedByName = "productIdToName")
    OrderItemResponse toResponse(OrderItem orderItem);

    @Named("productIdToName")
    default String productIdToName(Long productId) {
        // This is a placeholder, usually handled in service or with a decorator
        return "Product ID: " + productId;
    }
}
