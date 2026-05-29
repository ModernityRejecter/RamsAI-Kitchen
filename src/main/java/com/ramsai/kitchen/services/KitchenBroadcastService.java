package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KitchenBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    // Pushes a freshly-saved order to the kitchen board and to anyone tracking that order.
    // Called by controllers after the transaction commits.
    public void broadcastOrderUpdate(OrderResponse order) {
        if (order == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/kitchen", order);
        messagingTemplate.convertAndSend("/topic/orders/" + order.id(), order);
    }
}
