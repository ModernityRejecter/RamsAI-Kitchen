package com.ramsai.kitchen.services;

import com.ramsai.kitchen.config.GeminiConfig;
import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.models.dtos.*;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerAnalyticsService {

    private static final List<OrderStatus> NON_SALE_STATUSES = List.of(OrderStatus.DRAFT, OrderStatus.CANCELLED);
    private static final int DAILY_WINDOW_DAYS = 30;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport() {
        Map<Long, Double> ratings = reviewRepository.findAverageRatingsPerProduct().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Double) r[1]));

        List<SalesReportRow> rows = orderItemRepository.findProductSalesAggregates(NON_SALE_STATUSES).stream()
                .map(r -> new SalesReportRow(
                        (Long) r[0],
                        (String) r[1],
                        (String) r[2],
                        toLong(r[3]),
                        toMoney(r[4]),
                        round1(ratings.getOrDefault((Long) r[0], 0.0))
                ))
                .toList();

        List<CategorySalesReport> categories = rows.stream()
                .collect(Collectors.groupingBy(SalesReportRow::categoryName))
                .entrySet().stream()
                .map(e -> {
                    List<SalesReportRow> sorted = e.getValue().stream()
                            .sorted(Comparator.comparingLong(SalesReportRow::quantitySold).reversed())
                            .toList();
                    long qty = sorted.stream().mapToLong(SalesReportRow::quantitySold).sum();
                    BigDecimal revenue = sorted.stream()
                            .map(SalesReportRow::revenue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CategorySalesReport(e.getKey(), qty, revenue, sorted);
                })
                .sorted(Comparator.comparingLong(CategorySalesReport::totalQuantitySold).reversed())
                .toList();

        long totalUnits = rows.stream().mapToLong(SalesReportRow::quantitySold).sum();
        BigDecimal totalRevenue = rows.stream().map(SalesReportRow::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        double overallAvgRating = round1(rows.stream()
                .filter(r -> r.averageRating() > 0)
                .mapToDouble(SalesReportRow::averageRating)
                .average().orElse(0.0));

        List<SalesReportRow> topProducts = rows.stream()
                .sorted(Comparator.comparingLong(SalesReportRow::quantitySold).reversed())
                .limit(10)
                .toList();

        long totalOrders = orderRepository.countByStatusNotIn(NON_SALE_STATUSES);
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        List<DailySalesPoint> dailySales = buildDailySales();

        return new SalesReportResponse(totalUnits, totalRevenue, overallAvgRating,
                totalOrders, averageOrderValue, categories, topProducts, dailySales);
    }

    private List<DailySalesPoint> buildDailySales() {
        LocalDate start = LocalDate.now().minusDays(DAILY_WINDOW_DAYS - 1L);
        List<Order> orders = orderRepository.findAllByCreatedAtAfter(start.atStartOfDay());

        Map<LocalDate, Long> unitsByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();
        Map<LocalDate, Long> ordersByDay = new HashMap<>();

        for (Order o : orders) {
            if (o.getCreatedAt() == null || NON_SALE_STATUSES.contains(o.getStatus())) continue;
            LocalDate day = o.getCreatedAt().toLocalDate();
            ordersByDay.merge(day, 1L, Long::sum);
            if (o.getItems() == null) continue;
            for (OrderItem item : o.getItems()) {
                long qty = item.getQuantity() == null ? 0L : item.getQuantity();
                BigDecimal price = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
                unitsByDay.merge(day, qty, Long::sum);
                revenueByDay.merge(day, price.multiply(BigDecimal.valueOf(qty)), BigDecimal::add);
            }
        }

        List<DailySalesPoint> series = new ArrayList<>();
        for (int i = 0; i < DAILY_WINDOW_DAYS; i++) {
            LocalDate day = start.plusDays(i);
            series.add(new DailySalesPoint(
                    day,
                    unitsByDay.getOrDefault(day, 0L),
                    revenueByDay.getOrDefault(day, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                    ordersByDay.getOrDefault(day, 0L)));
        }
        return series;
    }

    @Transactional(readOnly = true)
    public AnalyticsAnswerResponse ask(String question) {
        String context = buildContext(getSalesReport());
        try {
            return callGemini(question, context);
        } catch (Exception e) {
            log.error("Analytics AI call failed", e);
            return new AnalyticsAnswerResponse(
                    "I'm unable to reach the analytics engine right now. Please try again in a moment.", null);
        }
    }

    private String buildContext(SalesReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("RESTAURANT SALES REPORT (excludes live carts and cancelled orders).\n");
        sb.append(String.format("All-time totals: %d units sold, $%s revenue across %d orders, " +
                        "average order value $%s, overall average rating %.1f/5.\n\n",
                report.totalUnitsSold(), report.totalRevenue().toPlainString(),
                report.totalOrders(), report.averageOrderValue().toPlainString(),
                report.overallAverageRating()));

        sb.append("Sales by category (sorted by units sold):\n");
        for (CategorySalesReport cat : report.categories()) {
            sb.append(String.format("- %s: %d units, $%s revenue\n",
                    cat.categoryName(), cat.totalQuantitySold(), cat.totalRevenue().toPlainString()));
            for (SalesReportRow p : cat.products()) {
                sb.append(String.format("    * %s: %d units, $%s, rating %.1f/5\n",
                        p.productName(), p.quantitySold(), p.revenue().toPlainString(), p.averageRating()));
            }
        }

        sb.append(String.format("\nDaily breakdown for the last %d days (date = units sold, revenue, orders):\n",
                DAILY_WINDOW_DAYS));
        for (DailySalesPoint d : report.dailySales()) {
            sb.append(String.format("%s = %d units, $%s, %d orders\n",
                    d.date(), d.units(), d.revenue().toPlainString(), d.orders()));
        }
        return sb.toString();
    }

    private AnalyticsAnswerResponse callGemini(String question, String context) {
        String url = geminiConfig.getUrl();
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("model", geminiConfig.getModel());
        uriVariables.put("key", geminiConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", Collections.singletonList(Collections.singletonMap("text",
                "You are a restaurant Business-Intelligence analyst assisting the manager. " +
                "Use the sales report data below to answer questions about product popularity, revenue, " +
                "categories, review ratings and day-by-day sales trends. The data includes a daily breakdown " +
                "for the last " + DAILY_WINDOW_DAYS + " days, so you CAN build time-series charts such as revenue, " +
                "units or orders per day. Use a 'line' chart for trends over time, and honour narrower windows " +
                "(e.g. 'last 7/14/20 days') by plotting only the most recent matching days in order. " +
                "You may also reason about figures derivable from the data (best/worst performers, averages, " +
                "comparisons, revenue share, growth, busiest days, etc.) even when not listed explicitly. " +
                "Keep written answers concise (a few sentences) and grounded in the numbers. " +
                "When the manager asks you to draw, plot, show, visualize or generate a chart/graph, " +
                "ALWAYS CALL the render_chart function with the requested chart type and the exact data to plot " +
                "(labels in chronological or ranked order, with matching numeric values). " +
                "Pick sensible labels and numeric values straight from the data.\n\n" +
                "DATA:\n" + context)));
        requestBody.put("system_instruction", systemInstruction);

        Map<String, Object> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("parts", Collections.singletonList(Collections.singletonMap("text", question)));
        requestBody.put("contents", Collections.singletonList(userTurn));

        requestBody.put("tools", Collections.singletonList(
                Collections.singletonMap("function_declarations",
                        Collections.singletonList(renderChartDeclaration()))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class, uriVariables);
        return parseResponse(response);
    }

    private Map<String, Object> renderChartDeclaration() {
        Map<String, Object> stringType = Collections.singletonMap("type", "STRING");
        Map<String, Object> numberType = Collections.singletonMap("type", "NUMBER");

        Map<String, Object> labels = new HashMap<>();
        labels.put("type", "ARRAY");
        labels.put("items", stringType);

        Map<String, Object> datasetProps = new HashMap<>();
        datasetProps.put("label", stringType);
        Map<String, Object> dataArray = new HashMap<>();
        dataArray.put("type", "ARRAY");
        dataArray.put("items", numberType);
        datasetProps.put("data", dataArray);

        Map<String, Object> datasetObject = new HashMap<>();
        datasetObject.put("type", "OBJECT");
        datasetObject.put("properties", datasetProps);
        datasetObject.put("required", List.of("label", "data"));

        Map<String, Object> datasets = new HashMap<>();
        datasets.put("type", "ARRAY");
        datasets.put("items", datasetObject);

        Map<String, Object> chartType = new HashMap<>();
        chartType.put("type", "STRING");
        chartType.put("enum", List.of("bar", "line", "pie", "doughnut", "radar", "polarArea"));

        Map<String, Object> properties = new HashMap<>();
        properties.put("type", chartType);
        properties.put("title", stringType);
        properties.put("labels", labels);
        properties.put("datasets", datasets);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "OBJECT");
        parameters.put("properties", properties);
        parameters.put("required", List.of("type", "title", "labels", "datasets"));

        Map<String, Object> declaration = new HashMap<>();
        declaration.put("name", "render_chart");
        declaration.put("description", "Renders a chart/graph for the manager from restaurant sales data.");
        declaration.put("parameters", parameters);
        return declaration;
    }

    @SuppressWarnings("unchecked")
    private AnalyticsAnswerResponse parseResponse(Map<String, Object> response) {
        if (response == null) {
            return new AnalyticsAnswerResponse("No response from the analytics engine.", null);
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return new AnalyticsAnswerResponse("No response from the analytics engine.", null);
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = content == null ? null : (List<Map<String, Object>>) content.get("parts");
        if (parts == null) {
            return new AnalyticsAnswerResponse("No response from the analytics engine.", null);
        }

        StringBuilder text = new StringBuilder();
        ChartSpec chart = null;
        for (Map<String, Object> part : parts) {
            if (part.containsKey("text")) {
                text.append((String) part.get("text"));
            }
            if (part.containsKey("functionCall")) {
                Map<String, Object> call = (Map<String, Object>) part.get("functionCall");
                if ("render_chart".equals(call.get("name"))) {
                    chart = parseChart((Map<String, Object>) call.get("args"));
                }
            }
        }

        String answer = text.toString().trim();
        if (answer.isEmpty()) {
            answer = chart != null
                    ? "Here is the chart you requested."
                    : "I couldn't produce an answer for that.";
        }
        return new AnalyticsAnswerResponse(answer, chart);
    }

    @SuppressWarnings("unchecked")
    private ChartSpec parseChart(Map<String, Object> args) {
        if (args == null) return null;
        String type = stringOr(args.get("type"), "bar");
        String title = stringOr(args.get("title"), "Chart");

        List<String> labels = ((List<Object>) args.getOrDefault("labels", List.of())).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        List<ChartDataset> datasets = new ArrayList<>();
        for (Object dObj : (List<Object>) args.getOrDefault("datasets", List.of())) {
            Map<String, Object> d = (Map<String, Object>) dObj;
            String label = stringOr(d.get("label"), "");
            List<Double> data = ((List<Object>) d.getOrDefault("data", List.of())).stream()
                    .map(ManagerAnalyticsService::toDouble)
                    .collect(Collectors.toList());
            datasets.add(new ChartDataset(label, data));
        }
        return new ChartSpec(type, title, labels, datasets);
    }

    private static String stringOr(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    private static long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static BigDecimal toMoney(Object o) {
        BigDecimal value;
        if (o == null) {
            value = BigDecimal.ZERO;
        } else if (o instanceof BigDecimal b) {
            value = b;
        } else if (o instanceof Number n) {
            value = BigDecimal.valueOf(n.doubleValue());
        } else {
            value = BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
