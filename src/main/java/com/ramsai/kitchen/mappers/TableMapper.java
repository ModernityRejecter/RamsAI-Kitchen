package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.TableResponse;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface TableMapper {

    @Mapping(target = "lastOrderTime", source = "lastOrderTime")
    TableResponse toResponse(RestaurantTable table, LocalDateTime lastOrderTime);
}
