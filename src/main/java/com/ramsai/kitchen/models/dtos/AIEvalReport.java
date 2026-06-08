package com.ramsai.kitchen.models.dtos;

import java.util.List;

public record AIEvalReport(
    String testCaseName,
    String inputPrompt,
    String aiResponse,
    boolean passed,
    double score, // 0.0 to 1.0
    String reasoning,
    List<String> failedCriteria
) {}
