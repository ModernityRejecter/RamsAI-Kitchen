package com.ramsai.kitchen.services;

import com.ramsai.kitchen.config.GeminiConfig;
import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.models.dtos.*;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
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

    // Fine-grained slices the model needs to chart "anything": every product and
    // category as its own daily units/revenue series (aligned to a shared date axis),
    // plus day-of-week and hour-of-day totals. Built from the same window of orders
    // that buildDailySales() uses, so no extra entities are loaded beyond one fetch.
    private String buildGranularContext() {
        LocalDate start = LocalDate.now().minusDays(DAILY_WINDOW_DAYS - 1L);
        List<Order> orders = orderRepository.findAllByCreatedAtAfter(start.atStartOfDay());

        List<LocalDate> axis = new ArrayList<>();
        Map<LocalDate, Integer> dayIndex = new HashMap<>();
        for (int i = 0; i < DAILY_WINDOW_DAYS; i++) {
            LocalDate day = start.plusDays(i);
            axis.add(day);
            dayIndex.put(day, i);
        }

        Map<String, long[]> productUnits = new TreeMap<>();
        Map<String, double[]> productRevenue = new TreeMap<>();
        Map<String, long[]> categoryUnits = new TreeMap<>();
        Map<String, double[]> categoryRevenue = new TreeMap<>();
        String[] dowNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        long[] dowOrders = new long[7];
        long[] dowUnits = new long[7];
        double[] dowRevenue = new double[7];
        long[] hourOrders = new long[24];
        long[] hourUnits = new long[24];
        double[] hourRevenue = new double[24];

        for (Order o : orders) {
            if (o.getCreatedAt() == null || NON_SALE_STATUSES.contains(o.getStatus())) continue;
            Integer di = dayIndex.get(o.getCreatedAt().toLocalDate());
            int dow = o.getCreatedAt().getDayOfWeek().getValue() - 1;
            int hour = o.getCreatedAt().getHour();
            dowOrders[dow]++;
            hourOrders[hour]++;
            if (o.getItems() == null) continue;
            for (OrderItem item : o.getItems()) {
                long qty = item.getQuantity() == null ? 0L : item.getQuantity();
                double revenue = (item.getUnitPrice() == null ? 0.0 : item.getUnitPrice().doubleValue()) * qty;
                dowUnits[dow] += qty;
                dowRevenue[dow] += revenue;
                hourUnits[hour] += qty;
                hourRevenue[hour] += revenue;
                Product p = item.getProduct();
                if (p == null || di == null) continue;
                String productName = p.getName();
                String categoryName = p.getCategory() != null ? p.getCategory().getName() : "Uncategorized";
                productUnits.computeIfAbsent(productName, k -> new long[DAILY_WINDOW_DAYS])[di] += qty;
                productRevenue.computeIfAbsent(productName, k -> new double[DAILY_WINDOW_DAYS])[di] += revenue;
                categoryUnits.computeIfAbsent(categoryName, k -> new long[DAILY_WINDOW_DAYS])[di] += qty;
                categoryRevenue.computeIfAbsent(categoryName, k -> new double[DAILY_WINDOW_DAYS])[di] += revenue;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nGRANULAR TIME-SERIES (every array aligns to this date axis, oldest first):\n");
        sb.append("Dates: ").append(axis.stream().map(LocalDate::toString).collect(Collectors.joining(", "))).append("\n");

        if (!productUnits.isEmpty()) {
            sb.append("\nUnits sold per day, per product (aligned to Dates):\n");
            productUnits.forEach((name, arr) -> sb.append("- ").append(name).append(": ").append(joinLongs(arr)).append("\n"));
            sb.append("\nRevenue ($) per day, per product (aligned to Dates):\n");
            productRevenue.forEach((name, arr) -> sb.append("- ").append(name).append(": ").append(joinMoney(arr)).append("\n"));
            sb.append("\nUnits sold per day, per category (aligned to Dates):\n");
            categoryUnits.forEach((name, arr) -> sb.append("- ").append(name).append(": ").append(joinLongs(arr)).append("\n"));
            sb.append("\nRevenue ($) per day, per category (aligned to Dates):\n");
            categoryRevenue.forEach((name, arr) -> sb.append("- ").append(name).append(": ").append(joinMoney(arr)).append("\n"));
        }

        sb.append("\nTotals by day-of-week (orders, units, revenue $):\n");
        for (int i = 0; i < 7; i++) {
            sb.append("- ").append(dowNames[i]).append(": ")
              .append(dowOrders[i]).append(" orders, ")
              .append(dowUnits[i]).append(" units, $").append(money(dowRevenue[i])).append("\n");
        }

        sb.append("\nTotals by hour-of-day (orders, units, revenue $):\n");
        for (int h = 0; h < 24; h++) {
            if (hourOrders[h] == 0) continue;
            sb.append("- ").append(String.format("%02d:00", h)).append(": ")
              .append(hourOrders[h]).append(" orders, ")
              .append(hourUnits[h]).append(" units, $").append(money(hourRevenue[h])).append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public AnalyticsAnswerResponse ask(String question) {
        String context = buildContext(getSalesReport()) + buildGranularContext();
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
                "You are a restaurant Business-Intelligence analyst assisting the manager. Answer questions " +
                "about sales, revenue, product and category performance, review ratings and trends, and DRAW " +
                "CHARTS on request. Everything you need is in the DATA section, covering the last " +
                DAILY_WINDOW_DAYS + " days:\n" +
                "- All-time-in-window totals plus per-category and per-product breakdowns (units, revenue, rating).\n" +
                "- A shared daily date axis and, aligned to it: overall units/revenue/orders per day; units AND " +
                "revenue per day for EVERY product; and units AND revenue per day for EVERY category.\n" +
                "- Totals broken down by day-of-week and by hour-of-day.\n\n" +
                "So you CAN plot virtually anything grounded in this data, for example: revenue, units or orders " +
                "over time (overall, for one specific product, or one specific category); several products or " +
                "categories compared over time (one dataset each, sharing the date labels); a product's or " +
                "category's share of total sales (pie/doughnut); product or category rankings (bar); ratings by " +
                "product; average order value; the busiest day-of-week or hour-of-day; best/worst performers; " +
                "growth between two periods; or the value on a single specific day. Honour narrower windows " +
                "(e.g. 'last 7/14 days') by using only the most recent matching days, in chronological order.\n\n" +
                "RULES:\n" +
                "- Pick the right chart type: 'line' for trends over time, 'bar' for ranking/comparing discrete " +
                "items, 'pie' or 'doughnut' for share-of-total.\n" +
                "- To compare multiple products/categories over time, return one dataset per series, all sharing " +
                "the same date labels. Labels and every dataset's data array MUST be the same length and aligned.\n" +
                "- Read numbers straight from the DATA; never invent values. If a product/category isn't listed, " +
                "it had no sales in the window (treat as zero).\n" +
                "- If the manager asks for something the data does NOT contain (profit, cost, margins, individual " +
                "customers, payment methods, ingredient/stock levels), briefly say it isn't available in the sales " +
                "data instead of guessing.\n" +
                "- Keep written answers concise (a few sentences) and grounded in the numbers.\n" +
                "- Whenever the manager asks you to draw, plot, show, visualise, graph or chart something, ALWAYS " +
                "CALL the render_chart function with the chosen chart type and the exact data to plot.\n\n" +
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
        labels.put("description", "Shared x-axis labels: dates (YYYY-MM-DD) for time series, or product/category " +
                "names for rankings and shares. Must have the same length and order as every dataset's data array.");

        Map<String, Object> seriesLabel = new HashMap<>();
        seriesLabel.put("type", "STRING");
        seriesLabel.put("description", "Series name shown in the legend (e.g. the product, category or metric).");
        Map<String, Object> datasetProps = new HashMap<>();
        datasetProps.put("label", seriesLabel);
        Map<String, Object> dataArray = new HashMap<>();
        dataArray.put("type", "ARRAY");
        dataArray.put("items", numberType);
        dataArray.put("description", "Numeric values aligned 1:1 with the top-level labels array (same length and order).");
        datasetProps.put("data", dataArray);

        Map<String, Object> datasetObject = new HashMap<>();
        datasetObject.put("type", "OBJECT");
        datasetObject.put("properties", datasetProps);
        datasetObject.put("required", List.of("label", "data"));

        Map<String, Object> datasets = new HashMap<>();
        datasets.put("type", "ARRAY");
        datasets.put("items", datasetObject);
        datasets.put("description", "One entry per series. Use several datasets (sharing the same labels) to " +
                "compare multiple products or categories on one chart.");

        Map<String, Object> chartType = new HashMap<>();
        chartType.put("type", "STRING");
        chartType.put("enum", List.of("bar", "line", "pie", "doughnut", "radar", "polarArea"));
        chartType.put("description", "Chart type: 'line' for trends over time, 'bar' for ranking/comparing items, " +
                "'pie' or 'doughnut' for share-of-total.");

        Map<String, Object> title = new HashMap<>();
        title.put("type", "STRING");
        title.put("description", "Concise, human-readable chart title.");

        Map<String, Object> properties = new HashMap<>();
        properties.put("type", chartType);
        properties.put("title", title);
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

    private static String joinLongs(long[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private static String joinMoney(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(money(arr[i]));
        }
        return sb.toString();
    }

    private static String money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
