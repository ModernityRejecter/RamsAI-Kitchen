package com.ramsai.kitchen.services;

import com.ramsai.kitchen.config.GeminiConfig;
import com.ramsai.kitchen.enums.SenderType;
import com.ramsai.kitchen.mappers.AIChatMapper;
import com.ramsai.kitchen.models.dtos.AIChatSessionResponse;
import com.ramsai.kitchen.models.dtos.AIMessageResponse;
import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
import com.ramsai.kitchen.models.entities.*;
import com.ramsai.kitchen.repositories.AIChatSessionRepository;
import com.ramsai.kitchen.repositories.AIMessageRepository;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIChatServiceTest {

    @Mock private AIChatSessionRepository sessionRepository;
    @Mock private AIMessageRepository messageRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductService productService;
    @Mock private AIChatMapper chatMapper;
    @Mock private GeminiConfig geminiConfig;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private AIChatService aiChatService;

    private AIChatSession session;

    @BeforeEach
    void setUp() {
        session = AIChatSession.builder().id(7L).build();
        // Echo the entity the service builds back through the mapper so tests can
        // assert on the content the service actually produced.
        lenient().when(chatMapper.toMessageResponse(any(AIMessage.class))).thenAnswer(inv -> {
            AIMessage m = inv.getArgument(0);
            return new AIMessageResponse(m.getId(), m.getSenderType(), m.getContent(), m.getTimestamp());
        });
    }

    // ---------------------------------------------------------------------
    // Agent 1 — Data Analyst (generateAnalysisContext)
    // ---------------------------------------------------------------------

    @Test
    void startNewSession_triggersDataAnalystAndPersistsSession() {
        User chef = new User();
        chef.setUsername("gordon");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(chef);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(orderRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(reviewRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(sessionRepository.save(any(AIChatSession.class))).thenReturn(session);
        when(chatMapper.toSessionResponse(session))
                .thenReturn(new AIChatSessionResponse(7L, null, null, List.of()));

        try (var holder = mockStatic(SecurityContextHolder.class)) {
            holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            AIChatSessionResponse response = aiChatService.startNewSession();

            assertNotNull(response);
            assertEquals(7L, response.id());
        }

        // Agent 1 pulls the trailing-window sales and review data.
        verify(orderRepository).findAllByCreatedAtAfter(any());
        verify(reviewRepository).findAllByCreatedAtAfter(any());
        verify(sessionRepository).save(any(AIChatSession.class));
    }

    @Test
    void sendMessage_feedsDataAnalystContextIntoSousChefPrompt() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(messageRepository.findAllBySessionIdOrderByTimestampAsc(7L)).thenReturn(List.of());

        // Two orders: Carbonara sells 5 units, Tiramisu sells 2 -> Carbonara must rank first.
        when(orderRepository.findAllByCreatedAtAfter(any()))
                .thenReturn(List.of(order(product("Carbonara"), 3), order(product("Carbonara"), 2),
                        order(product("Tiramisu"), 2)));
        when(reviewRepository.findAllByCreatedAtAfter(any()))
                .thenReturn(List.of(review(product("Carbonara"), 5, "Best in town")));

        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(textResponse("ok"));

        aiChatService.sendMessage(7L, "Suggest a dish");

        String systemInstruction = capturePromptSystemInstruction();
        assertTrue(systemInstruction.contains("Top selling products"), systemInstruction);
        assertTrue(systemInstruction.contains("Carbonara: 5 units"), systemInstruction);
        assertTrue(systemInstruction.contains("Tiramisu: 2 units"), systemInstruction);
        assertTrue(systemInstruction.contains("Best in town"), systemInstruction);
        assertTrue(systemInstruction.contains("(5/5 stars)"), systemInstruction);
    }

    // ---------------------------------------------------------------------
    // Agent 2 — Sous-Chef (Gemini conversation + tool use)
    // ---------------------------------------------------------------------

    @Test
    void sendMessage_persistsUserMessageThenSousChefReply() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(messageRepository.findAllBySessionIdOrderByTimestampAsc(7L)).thenReturn(List.of());
        when(orderRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(reviewRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(textResponse("How about a saffron risotto?"));

        AIMessageResponse response = aiChatService.sendMessage(7L, "Give me an idea");

        assertEquals("How about a saffron risotto?", response.content());
        assertEquals(SenderType.AI, response.senderType());

        ArgumentCaptor<AIMessage> saved = ArgumentCaptor.forClass(AIMessage.class);
        verify(messageRepository, times(2)).save(saved.capture());
        AIMessage userMsg = saved.getAllValues().get(0);
        AIMessage aiMsg = saved.getAllValues().get(1);
        assertEquals(SenderType.USER, userMsg.getSenderType());
        assertEquals("Give me an idea", userMsg.getContent());
        assertEquals(SenderType.AI, aiMsg.getSenderType());
        assertEquals("How about a saffron risotto?", aiMsg.getContent());
    }

    @Test
    void sendMessage_sendsConversationHistoryWithCorrectRoles() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(orderRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(reviewRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(messageRepository.findAllBySessionIdOrderByTimestampAsc(7L)).thenReturn(List.of(
                AIMessage.builder().senderType(SenderType.USER).content("hi").build(),
                AIMessage.builder().senderType(SenderType.AI).content("chef here").build()));
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(textResponse("ok"));

        aiChatService.sendMessage(7L, "next");

        Map<String, Object> body = capturePromptBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) body.get("contents");
        assertEquals(2, contents.size());
        assertEquals("user", contents.get(0).get("role"));
        assertEquals("model", contents.get(1).get("role"));
    }

    @Test
    void sendMessage_declaresAddProductTool() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(messageRepository.findAllBySessionIdOrderByTimestampAsc(7L)).thenReturn(List.of());
        when(orderRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(reviewRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(textResponse("ok"));

        aiChatService.sendMessage(7L, "hello");

        Map<String, Object> body = capturePromptBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) body.get("tools");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> declarations =
                (List<Map<String, Object>>) tools.get(0).get("function_declarations");
        assertEquals("add_product", declarations.get(0).get("name"));
    }

    @Test
    void sendMessage_functionCallCreatesProductAndAnnouncesIt() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(messageRepository.findAllBySessionIdOrderByTimestampAsc(7L)).thenReturn(List.of());
        when(orderRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(reviewRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());

        Map<String, Object> args = new HashMap<>();
        args.put("name", "Truffle Risotto");
        args.put("description", "Creamy arborio rice with black truffle");
        args.put("basePrice", 24.5);
        args.put("categoryId", 2);
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(functionCallResponse("add_product", args));

        AIMessageResponse response = aiChatService.sendMessage(7L, "Add that dish");

        ArgumentCaptor<ProductCreateRequest> req = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).createProduct(req.capture());
        ProductCreateRequest created = req.getValue();
        assertEquals("Truffle Risotto", created.name());
        assertEquals("Creamy arborio rice with black truffle", created.description());
        assertEquals(0, BigDecimal.valueOf(24.5).compareTo(created.basePrice()));
        assertEquals(2L, created.categoryId());

        assertTrue(response.content().contains("Recipe added to the database for approval"),
                response.content());
    }

    @Test
    void sendMessage_whenGeminiFailsReturnsGracefulFallback() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(messageRepository.findAllBySessionIdOrderByTimestampAsc(7L)).thenReturn(List.of());
        when(orderRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(reviewRepository.findAllByCreatedAtAfter(any())).thenReturn(List.of());
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenThrow(new RuntimeException("connection refused"));

        AIMessageResponse response = aiChatService.sendMessage(7L, "Help");

        assertTrue(response.content().contains("trouble connecting"), response.content());
        verify(productService, never()).createProduct(any());
    }

    @Test
    void sendMessage_unknownSession_throws() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> aiChatService.sendMessage(99L, "Hi"));
        verifyNoInteractions(restTemplate);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Map<String, Object> capturePromptBody() {
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(any(), captor.capture(), eq(Map.class), anyMap());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        return body;
    }

    private String capturePromptSystemInstruction() {
        Map<String, Object> body = capturePromptBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> systemInstruction = (Map<String, Object>) body.get("system_instruction");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) systemInstruction.get("parts");
        return (String) parts.get(0).get("text");
    }

    private static Product product(String name) {
        return Product.builder().name(name).build();
    }

    private static Order order(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        Order order = new Order();
        order.setItems(new ArrayList<>(List.of(item)));
        return order;
    }

    private static Review review(Product product, int rating, String comment) {
        return Review.builder().product(product).rating(rating).comment(comment).build();
    }

    private static Map<String, Object> textResponse(String text) {
        return candidates(List.of(map("text", text)));
    }

    private static Map<String, Object> functionCallResponse(String name, Map<String, Object> args) {
        Map<String, Object> call = new HashMap<>();
        call.put("name", name);
        call.put("args", args);
        return candidates(List.of(map("functionCall", call)));
    }

    private static Map<String, Object> candidates(List<Map<String, Object>> parts) {
        Map<String, Object> candidate = map("content", map("parts", parts));
        return map("candidates", List.of(candidate));
    }

    private static Map<String, Object> map(String key, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return m;
    }
}
