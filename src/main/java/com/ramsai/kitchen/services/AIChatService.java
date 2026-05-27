package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.SenderType;
import com.ramsai.kitchen.models.entities.AIChatSession;
import com.ramsai.kitchen.models.entities.AIMessage;
import com.ramsai.kitchen.repositories.AIChatSessionRepository;
import com.ramsai.kitchen.repositories.AIMessageRepository;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIChatService {

    private final AIChatSessionRepository sessionRepository;
    private final AIMessageRepository messageRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public AIChatSession createSession(Long userId) {
        AIChatSession session = new AIChatSession();
        session.setUserId(userId);
        session.setStartedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Transactional
    public List<AIMessage> getMessages(Long sessionId) {
        return messageRepository.findAllBySessionIdOrderByTimestampAsc(sessionId);
    }

    @Transactional
    public AIMessage sendMessage(Long sessionId, String content) {
        AIChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Save user message
        AIMessage userMessage = new AIMessage();
        userMessage.setSession(session);
        userMessage.setSenderType(SenderType.USER);
        userMessage.setContent(content);
        userMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(userMessage);

        // Generate AI response
        String aiResponseContent = generateAIResponse(content);
        
        AIMessage aiMessage = new AIMessage();
        aiMessage.setSession(session);
        aiMessage.setSenderType(SenderType.AI);
        aiMessage.setContent(aiResponseContent);
        aiMessage.setTimestamp(LocalDateTime.now());
        return messageRepository.save(aiMessage);
    }

    private String generateAIResponse(String userContent) {
        String content = userContent.toLowerCase();
        if (content.contains("recipe") || content.contains("retetă") || content.contains("popular")) {
            return "Based on the most popular products (Classic Cheeseburger and Carbonara), I suggest a new recipe: 'Truffle Burger Pasta'. It combines our premium beef with a creamy truffle sauce.";
        }
        return "I'm your Kitchen AI Assistant. How can I help you today? I can suggest new recipes based on popular items or help you with kitchen workflows.";
    }

    public List<AIChatSession> getUserSessions(Long userId) {
        return sessionRepository.findAllByUserIdOrderByStartedAtDesc(userId);
    }
}
