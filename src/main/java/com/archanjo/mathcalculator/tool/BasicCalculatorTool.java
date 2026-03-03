package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BasicCalculatorTool {
    @Tool(description = "Add two numbers. Returns exact result.")
    public String add(
            @ToolParam(description = "First number") final String first,
            @ToolParam(description = "Second number") final String second) {
        return new BigDecimal(first).add(new BigDecimal(second)).toPlainString();
    }

    @Tool(description = "Subtract second from first. Returns exact result.")
    public String subtract(
            @ToolParam(description = "Minuend") final String first,
            @ToolParam(description = "Subtrahend") final String second) {
        return new BigDecimal(first).subtract(new BigDecimal(second)).toPlainString();
    }

    @Tool(description = "Multiply two numbers. Returns exact result.")
    public String multiply(
            @ToolParam(description = "First factor") final String first,
            @ToolParam(description = "Second factor") final String second) {
        return new BigDecimal(first).multiply(new BigDecimal(second)).toPlainString();
    }

    @Tool(description = "Divide first by second. 20-digit precision.")
    public String divide(
            @ToolParam(description = "Dividend") final String first,
            @ToolParam(description = "Divisor (non-zero)") final String second) {
        final BigDecimal divisor = new BigDecimal(second);
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
        return new BigDecimal(first)
                .divide(divisor, 20, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    @Tool(description = "Raise base to exponent. Returns exact result.")
    public String power(
            @ToolParam(description = "Base number") final String base,
            @ToolParam(description = "Non-negative integer exponent") final String exponent) {
        final int exp = Integer.parseInt(exponent);
        if (exp < 0) {
            throw new IllegalArgumentException("Exponent must be a non-negative integer");
        }
        return new BigDecimal(base).pow(exp).toPlainString();
    }

    @Tool(description = "Compute remainder of first divided by second.")
    public String modulo(
            @ToolParam(description = "Dividend") final String first,
            @ToolParam(description = "Divisor (non-zero)") final String second) {
        final BigDecimal divisor = new BigDecimal(second);
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
        return new BigDecimal(first).remainder(divisor).toPlainString();
    }

    @Tool(description = "Compute absolute value of a number.")
    public String abs(
            @ToolParam(description = "Number") final String value) {
        return new BigDecimal(value).abs().toPlainString();
    }
}
