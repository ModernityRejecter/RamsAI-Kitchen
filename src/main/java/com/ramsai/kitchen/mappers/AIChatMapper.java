package com.ramsai.kitchen.mappers;

import com.ramsai.kitchen.models.dtos.AIChatSessionResponse;
import com.ramsai.kitchen.models.dtos.AIMessageResponse;
import com.ramsai.kitchen.models.entities.AIChatSession;
import com.ramsai.kitchen.models.entities.AIMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AIChatMapper {

    AIChatSessionResponse toSessionResponse(AIChatSession session);

    AIMessageResponse toMessageResponse(AIMessage message);

    List<AIMessageResponse> toMessageResponseList(List<AIMessage> messages);
}
