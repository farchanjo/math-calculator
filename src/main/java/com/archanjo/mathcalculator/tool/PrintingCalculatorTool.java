package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PrintingCalculatorTool {

    private static final int DISPLAY_SCALE = 2;
    private static final int DIVISION_SCALE = 20;
    private static final int NUMBER_WIDTH = 14;
    private static final String SEPARATOR = "       --------";
    @Tool(description = "Tape calculator. Returns printed tape with running totals.")
    public String calculateWithTape(
            @ToolParam(description = "JSON array of {op, value}. ops: +,-,*,/,=,C,T")
            final String operations) {

        if (operations == null || operations.isBlank()) {
            throw new IllegalArgumentException("Operations must not be null or empty");
        }

        final List<Entry> entries = parseEntries(operations);
        return buildTape(entries);
    }

    private String buildTape(final List<Entry> entries) {
        final BigDecimal[] parsedValues = parseValues(entries);
        BigDecimal total = BigDecimal.ZERO;
        final StringBuilder tape = new StringBuilder();

        for (int idx = 0; idx < entries.size(); idx++) {
            final Entry entry = entries.get(idx);
            final BigDecimal value = parsedValues[idx];
            if (needsValue(entry.operation)) {
                total = applyArithmetic(total, value, entry.operation);
                appendLine(tape, value, entry.operation);
            } else {
                total = applyControl(tape, total, entry.operation);
            }
        }

        return tape.toString();
    }

    private BigDecimal applyArithmetic(final BigDecimal total,
            final BigDecimal value, final String operation) {
        return switch (operation) {
            case "+" -> total.add(value);
            case "-" -> total.subtract(value);
            case "*" -> total.multiply(value);
            case "/" -> {
                if (value.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("Division by zero");
                }
                yield total.divide(value, DIVISION_SCALE, RoundingMode.HALF_UP);
            }
            default -> throw new IllegalArgumentException(
                    "Unknown arithmetic operation: " + operation);
        };
    }

    private BigDecimal applyControl(final StringBuilder tape,
            final BigDecimal total, final String operation) {
        return switch (operation) {
            case "=" -> {
                tape.append(SEPARATOR).append('\n');
                appendLine(tape, total, "=");
                yield total;
            }
            case "C" -> {
                appendLine(tape, BigDecimal.ZERO, "C");
                yield BigDecimal.ZERO;
            }
            case "T" -> {
                tape.append(SEPARATOR).append('\n');
                appendLine(tape, total, "T");
                yield BigDecimal.ZERO;
            }
            default -> throw new IllegalArgumentException(
                    "Unknown control operation: " + operation);
        };
    }

    private void appendLine(final StringBuilder tape, final BigDecimal value,
            final String operation) {
        final String formatted = value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                .toPlainString();
        tape.append(String.format("%" + NUMBER_WIDTH + "s  %s", formatted, operation))
                .append('\n');
    }

    private List<Entry> parseEntries(final String json) {
        final String trimmed = json.strip();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Operations must be a JSON array");
        }

        final String inner = trimmed.substring(1, trimmed.length() - 1).strip();
        final List<Entry> result = new ArrayList<>();
        if (!inner.isEmpty()) {
            final String[] parts = inner.split("\\},\\s*\\{");
            for (final String part : parts) {
                final String clean = part.replace("{", "").replace("}", "").strip();
                final String operation = extractJsonValue(clean, "op");
                final String value = extractJsonValue(clean, "value");
                result.add(new Entry(operation, value));
            }
        }
        return result;
    }

    private String extractJsonValue(final String object, final String key) {
        final String search = "\"" + key + "\"";
        final int keyIndex = object.indexOf(search);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Missing field: " + key);
        }

        final int colonIndex = object.indexOf(':', keyIndex + search.length());
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Malformed JSON near field: " + key);
        }

        final String afterColon = object.substring(colonIndex + 1).strip();
        final String result;
        if (afterColon.startsWith("\"")) {
            final int endQuote = afterColon.indexOf('"', 1);
            if (endQuote < 0) {
                throw new IllegalArgumentException(
                        "Unterminated string for field: " + key);
            }
            result = afterColon.substring(1, endQuote);
        } else {
            final int end = afterColon.indexOf(',');
            result = end < 0
                    ? afterColon.strip()
                    : afterColon.substring(0, end).strip();
        }
        return result;
    }

    private BigDecimal[] parseValues(final List<Entry> entries) {
        return entries.stream()
                .map(entry -> needsValue(entry.operation)
                        ? new BigDecimal(entry.value) : null)
                .toArray(BigDecimal[]::new);
    }

    private boolean needsValue(final String operation) {
        return "+".equals(operation) || "-".equals(operation)
                || "*".equals(operation) || "/".equals(operation);
    }

    private record Entry(String operation, String value) {
    }
}
