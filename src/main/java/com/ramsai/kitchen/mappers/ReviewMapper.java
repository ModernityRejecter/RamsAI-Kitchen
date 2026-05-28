package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.ReviewResponse;
import com.ramsai.kitchen.models.entities.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.username")
    ReviewResponse toResponse(Review review);
}
