package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.TableResponse;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface TableMapper {

    @Mapping(target = "id", source = "table.id")
    @Mapping(target = "tableNumber", source = "table.tableNumber")
    @Mapping(target = "status", source = "table.status")
    @Mapping(target = "xPos", source = "table.xpos")
    @Mapping(target = "yPos", source = "table.ypos")
    @Mapping(target = "lastOrderTime", source = "lastOrderTime")
    TableResponse toResponse(RestaurantTable table, LocalDateTime lastOrderTime);
}
