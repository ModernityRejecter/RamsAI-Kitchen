package com.ramsai.kitchen.services;

import com.ramsai.kitchen.config.GeminiConfig;
import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.enums.SenderType;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.mappers.AIChatMapper;
import com.ramsai.kitchen.models.dtos.*;
import com.ramsai.kitchen.models.entities.*;
import com.ramsai.kitchen.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {

    private final AIChatSessionRepository sessionRepository;
    private final AIMessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final AIChatMapper chatMapper;
    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;

    @Transactional
    public AIChatSessionResponse startNewSession() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Starting new AI chat session for user: {}", currentUser.getUsername());

        String analysisContext = generateAnalysisContext();
        log.info("Agent 1 (Data Analyst) triggered. Data retrieved and context generated.");

        AIChatSession session = AIChatSession.builder()
                .user(currentUser)
                .startedAt(LocalDateTime.now())
                .build();
        
        session = sessionRepository.save(session);

        // Initial system message or hidden context could be stored if needed, 
        // but here we just return the session.
        return chatMapper.toSessionResponse(session);
    }

    @Transactional
    public List<AIChatSessionResponse> getUserSessions() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return sessionRepository.findAllByUserIdOrderByStartedAtDesc(currentUser.getId()).stream()
                .map(chatMapper::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AIMessageResponse sendMessage(Long sessionId, String content) {
        AIChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Save user message
        AIMessage userMessage = AIMessage.builder()
                .session(session)
                .senderType(SenderType.USER)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(userMessage);

        // Call Gemini
        String aiResponseContent = callGemini(session, content);

        // Save AI message
        AIMessage aiMessage = AIMessage.builder()
                .session(session)
                .senderType(SenderType.AI)
                .content(aiResponseContent)
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(aiMessage);

        return chatMapper.toMessageResponse(aiMessage);
    }

    private String generateAnalysisContext() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Order> recentOrders = orderRepository.findAllByCreatedAtAfter(sevenDaysAgo);
        List<Review> recentReviews = reviewRepository.findAllByCreatedAtAfter(sevenDaysAgo);

        Map<String, Integer> productCounts = new HashMap<>();
        for (Order o : recentOrders) {
            for (OrderItem item : o.getItems()) {
                String name = item.getProduct().getName();
                productCounts.put(name, productCounts.getOrDefault(name, 0) + item.getQuantity());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Analysis of the last 7 days:\n");
        sb.append("Top selling products:\n");
        productCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append(" units\n"));

        sb.append("\nRecent customer feedback:\n");
        recentReviews.stream()
                .limit(10)
                .forEach(r -> sb.append("- ").append(r.getProduct().getName()).append(" (").append(r.getRating()).append("/5 stars): ").append(r.getComment()).append("\n"));

        return sb.toString();
    }

    private String callGemini(AIChatSession session, String userMessage) {
        String url = geminiConfig.getUrl();
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("model", geminiConfig.getModel());
        uriVariables.put("key", geminiConfig.getApiKey());

        String analysisContext = generateAnalysisContext();
        List<AIMessage> history = messageRepository.findAllBySessionIdOrderByTimestampAsc(session.getId());

        Map<String, Object> requestBody = new HashMap<>();
        
        // System instruction
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", Collections.singletonList(Collections.singletonMap("text", 
            "### PERSONA\n" +
            "You are a professional Sous-Chef AI assistant for 'RamsAI Kitchen'. Your tone is professional, helpful, and culinary-focused.\n\n" +
            "### CORE DUTIES\n" +
            "- Analyze sales and review data to suggest new recipes.\n" +
            "- Use the 'add_product' tool ONLY when the head chef confirms a recipe.\n\n" +
            "### STRICT BOUNDARIES & SAFETY\n" +
            "- You ONLY discuss food, recipes, kitchen operations, and restaurant data.\n" +
            "- If the user asks about ANY topic outside the kitchen (e.g., programming, hacking, general advice, sports, etc.), you MUST politely refuse.\n" +
            "- Example Refusal: 'I apologize, Chef, but my expertise is limited to our kitchen operations and culinary excellence. I cannot assist with that request.'\n" +
            "- NEVER break character or provide assistance for out-of-scope tasks.\n\n" +
            "### CURRENT DATA CONTEXT\n" +
            analysisContext
        )));
        requestBody.put("system_instruction", systemInstruction);

        // Contents (History + Current Message)
        List<Map<String, Object>> contents = new ArrayList<>();
        for (AIMessage msg : history) {
            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("role", msg.getSenderType() == SenderType.USER ? "user" : "model");
            contentPart.put("parts", Collections.singletonList(Collections.singletonMap("text", msg.getContent())));
            contents.add(contentPart);
        }
        requestBody.put("contents", contents);

        // Tools
        Map<String, Object> tool = new HashMap<>();
        Map<String, Object> functionDecl = new HashMap<>();
        functionDecl.put("name", "add_product");
        functionDecl.put("description", "Adds a new recipe/product to the system with PENDING status.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "OBJECT");
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", Collections.singletonMap("type", "STRING"));
        properties.put("description", Collections.singletonMap("type", "STRING"));
        properties.put("basePrice", Collections.singletonMap("type", "NUMBER"));
        properties.put("categoryId", Collections.singletonMap("type", "INTEGER"));
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList("name", "basePrice", "categoryId"));
        functionDecl.put("parameters", parameters);
        
        tool.put("function_declarations", Collections.singletonList(functionDecl));
        requestBody.put("tools", Collections.singletonList(tool));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Sending payload to Gemini: {}", requestBody);
        
        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class, uriVariables);
            log.debug("Received payload from Gemini: {}", response);
            
            return processGeminiResponse(response);
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return "I'm sorry, I'm having trouble connecting to my creative circuits right now. Please try again later.";
        }
    }

    private String processGeminiResponse(Map<String, Object> response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "No response from AI.";

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> part : parts) {
            if (part.containsKey("text")) {
                result.append(part.get("text"));
            }
            if (part.containsKey("functionCall")) {
                Map<String, Object> call = (Map<String, Object>) part.get("functionCall");
                if ("add_product".equals(call.get("name"))) {
                    handleFunctionCall((Map<String, Object>) call.get("args"));
                    result.append("\n[System: Recipe added to the database for approval.]");
                }
            }
        }
        return result.toString();
    }

    private void handleFunctionCall(Map<String, Object> args) {
        log.info("Gemini triggered function call 'add_product' with args: {}", args);
        
        String name = (String) args.get("name");
        String description = (String) args.get("description");
        // Handle number conversion carefully
        Object priceObj = args.get("basePrice");
        java.math.BigDecimal basePrice = priceObj instanceof Number 
                ? java.math.BigDecimal.valueOf(((Number) priceObj).doubleValue()) 
                : java.math.BigDecimal.ZERO;
        
        Long categoryId = ((Number) args.get("categoryId")).longValue();

        ProductCreateRequest request = new ProductCreateRequest(
                name,
                description,
                basePrice,
                categoryId,
                false, // isSpecialOffer
                false, // isDailyRecipe
                null   // discountPrice
        );

        productService.createProduct(request);
    }
}
