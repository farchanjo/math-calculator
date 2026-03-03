package com.archanjo.mathcalculator.tool;

import java.math.BigInteger;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ScientificCalculatorTool {
    @Tool(description = "Compute square root of a number.")
    public double sqrt(
            @ToolParam(description = "Non-negative number")
            final double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of a negative number");
        }
        return StrictMath.sqrt(number);
    }

    @Tool(description = "Compute natural logarithm (ln) of a number.")
    public double log(
            @ToolParam(description = "Positive number")
            final double number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Cannot calculate logarithm of a non-positive number");
        }
        return StrictMath.log(number);
    }

    @Tool(description = "Compute base-10 logarithm of a number.")
    public double log10(
            @ToolParam(description = "Positive number")
            final double number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Cannot calculate logarithm of a non-positive number");
        }
        return StrictMath.log10(number);
    }

    @Tool(description = "Compute factorial (n!). Range: 0 to 20.")
    public String factorial(
            @ToolParam(description = "Integer from 0 to 20")
            final int num) {
        if (num < 0 || num > 20) {
            throw new IllegalArgumentException("Factorial input must be between 0 and 20 inclusive");
        }
        BigInteger result = BigInteger.ONE;
        for (int idx = 2; idx <= num; idx++) {
            result = result.multiply(BigInteger.valueOf(idx));
        }
        return result.toString();
    }

    @Tool(description = "Compute sine of an angle in degrees.")
    public double sin(
            @ToolParam(description = "Angle in degrees") final double degrees) {
        return StrictMath.sin(StrictMath.toRadians(degrees));
    }

    @Tool(description = "Compute cosine of an angle in degrees.")
    public double cos(
            @ToolParam(description = "Angle in degrees") final double degrees) {
        return StrictMath.cos(StrictMath.toRadians(degrees));
    }

    @Tool(description = "Compute tangent of an angle in degrees.")
    public double tan(
            @ToolParam(description = "Angle in degrees") final double degrees) {
        return StrictMath.tan(StrictMath.toRadians(degrees));
    }
}
