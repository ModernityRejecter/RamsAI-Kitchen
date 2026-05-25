package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.models.entities.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
