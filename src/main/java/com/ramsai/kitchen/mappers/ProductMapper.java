package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "isSpecialOffer", source = "specialOffer")
    @Mapping(target = "isDailyRecipe", source = "dailyRecipe")
    @Mapping(target = "approvalStatus", source = "approvalStatus")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "rejectionFeedback", source = "rejectionFeedback")
    ProductResponse toResponse(Product product);
}
