package com.ramsai.kitchen.services;

import com.ramsai.kitchen.config.GeminiConfig;
import com.ramsai.kitchen.models.dtos.AnalyticsAnswerResponse;
import com.ramsai.kitchen.models.dtos.CategorySalesReport;
import com.ramsai.kitchen.models.dtos.SalesReportResponse;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerAnalyticsServiceTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private GeminiConfig geminiConfig;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private ManagerAnalyticsService service;

    private void stubSalesData() {
        when(reviewRepository.findAverageRatingsPerProduct()).thenReturn(List.of(
                new Object[]{1L, 4.5}, new Object[]{3L, 5.0}));
        when(orderItemRepository.findProductSalesAggregates(anyList())).thenReturn(List.of(
                new Object[]{1L, "Carbonara", "Pasta", 5L, new BigDecimal("50.00")},
                new Object[]{2L, "Tiramisu", "Dessert", 2L, new BigDecimal("16.00")},
                new Object[]{3L, "Lasagna", "Pasta", 8L, new BigDecimal("96.00")}));
    }

    // ---- Deterministic sales report (Story 10 acceptance criteria) ----

    @Test
    void getSalesReport_groupsByCategorySortsByPopularityAndMergesRatings() {
        stubSalesData();

        SalesReportResponse report = service.getSalesReport();

        // Categories sorted by total units sold desc -> Pasta (13) before Dessert (2)
        assertEquals(2, report.categories().size());
        CategorySalesReport pasta = report.categories().get(0);
        assertEquals("Pasta", pasta.categoryName());
        assertEquals(13, pasta.totalQuantitySold());
        assertEquals(0, new BigDecimal("146.00").compareTo(pasta.totalRevenue()));

        // Products within a category sorted by units sold desc -> Lasagna (8) before Carbonara (5)
        assertEquals("Lasagna", pasta.products().get(0).productName());
        assertEquals(5.0, pasta.products().get(0).averageRating());
        assertEquals("Carbonara", pasta.products().get(1).productName());
        assertEquals(4.5, pasta.products().get(1).averageRating());

        assertEquals("Dessert", report.categories().get(1).categoryName());
    }

    @Test
    void getSalesReport_computesTotalsAndTopProducts() {
        stubSalesData();

        SalesReportResponse report = service.getSalesReport();

        assertEquals(15, report.totalUnitsSold());
        assertEquals(0, new BigDecimal("162.00").compareTo(report.totalRevenue()));
        // Average of rated products (4.5, 5.0) rounded to 1 decimal; the unrated one is excluded.
        assertEquals(4.8, report.overallAverageRating());
        assertEquals("Lasagna", report.topProducts().get(0).productName());

        // Daily series is always a complete, zero-filled 30-day window for the trend charts.
        assertEquals(30, report.dailySales().size());
        assertEquals(0, report.totalOrders());
    }

    // ---- AI analyst (Gemini-backed) ----

    @Test
    void ask_returnsTextAnswerWhenNoChartRequested() {
        stubSalesData();
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(textResponse("Pasta is your strongest category."));

        AnalyticsAnswerResponse answer = service.ask("Which category sells best?");

        assertEquals("Pasta is your strongest category.", answer.answer());
        assertNull(answer.chart());
    }

    @Test
    void ask_parsesRenderChartFunctionCallIntoChartSpec() {
        stubSalesData();

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Revenue");
        dataset.put("data", List.of(146.0, 16.0));
        Map<String, Object> args = new HashMap<>();
        args.put("type", "pie");
        args.put("title", "Revenue by category");
        args.put("labels", List.of("Pasta", "Dessert"));
        args.put("datasets", List.of(dataset));

        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenReturn(functionCallResponse("render_chart", args));

        AnalyticsAnswerResponse answer = service.ask("Show a pie chart of revenue by category");

        assertNotNull(answer.chart());
        assertEquals("pie", answer.chart().type());
        assertEquals("Revenue by category", answer.chart().title());
        assertEquals(List.of("Pasta", "Dessert"), answer.chart().labels());
        assertEquals("Revenue", answer.chart().datasets().get(0).label());
        assertEquals(List.of(146.0, 16.0), answer.chart().datasets().get(0).data());
    }

    @Test
    void ask_returnsGracefulFallbackWhenGeminiFails() {
        stubSalesData();
        when(restTemplate.postForObject(any(), any(), eq(Map.class), anyMap()))
                .thenThrow(new RuntimeException("network down"));

        AnalyticsAnswerResponse answer = service.ask("anything");

        assertTrue(answer.answer().toLowerCase().contains("unable to reach"), answer.answer());
        assertNull(answer.chart());
    }

    // ---- helpers (mirror the Gemini response envelope) ----

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
