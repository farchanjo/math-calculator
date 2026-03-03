package com.archanjo.mathcalculator.tool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.ExpressionEvaluator;

@Component
public class ProgrammableCalculatorTool {
    @Tool(description = "Evaluate a math expression. Supports +,-,*,/,^,% and functions.")
    public String evaluate(
            @ToolParam(description = "Expression, e.g. 'sin(45)+2^3'") final String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression must not be empty");
        }
        final double result = ExpressionEvaluator.evaluate(expression);
        return String.valueOf(result);
    }

    @Tool(description = "Evaluate a math expression with variables.")
    public String evaluateWithVariables(
            @ToolParam(description = "Expression, e.g. '2*x + y'") final String expression,
            @ToolParam(description = "JSON map, e.g. {\"x\":3.0,\"y\":1.0}")
            final String variables) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression must not be empty");
        }
        if (variables == null || variables.isBlank()) {
            throw new IllegalArgumentException("Variables must not be empty");
        }
        final Map<String, Double> variablesMap = parseVariables(variables);
        final double result = ExpressionEvaluator.evaluate(expression, variablesMap);
        return String.valueOf(result);
    }

    private Map<String, Double> parseVariables(final String json) {
        final Map<String, Double> map = new ConcurrentHashMap<>();
        final String stripped = json.trim();
        final String content = stripped.substring(1, stripped.length() - 1).trim();
        if (!content.isEmpty()) {
            final String[] entries = content.split(",");
            for (final String entry : entries) {
                final String[] pair = entry.split(":");
                final String key = pair[0].trim().replace("\"", "");
                final double value = Double.parseDouble(pair[1].trim());
                map.put(key, value);
            }
        }
        return map;
    }
}
