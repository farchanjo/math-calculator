package com.archanjo.mathcalculator.tool;

import java.math.BigInteger;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ScientificCalculatorTool {

    private static final double SQRT2_OVER_2 = StrictMath.sqrt(2.0) / 2.0;
    private static final double SQRT3_OVER_2 = StrictMath.sqrt(3.0) / 2.0;
    private static final double SQRT3 = StrictMath.sqrt(3.0);
    private static final double ONE_OVER_SQRT3 = 1.0 / StrictMath.sqrt(3.0);
    private static final int FULL_CIRCLE = 360;

    private static final Map<Integer, Double> SIN_TABLE = Map.ofEntries(
            Map.entry(0, 0.0), Map.entry(180, 0.0),
            Map.entry(30, 0.5), Map.entry(150, 0.5),
            Map.entry(45, SQRT2_OVER_2), Map.entry(135, SQRT2_OVER_2),
            Map.entry(60, SQRT3_OVER_2), Map.entry(120, SQRT3_OVER_2),
            Map.entry(90, 1.0),
            Map.entry(210, -0.5), Map.entry(330, -0.5),
            Map.entry(225, -SQRT2_OVER_2), Map.entry(315, -SQRT2_OVER_2),
            Map.entry(240, -SQRT3_OVER_2), Map.entry(300, -SQRT3_OVER_2),
            Map.entry(270, -1.0));

    private static final Map<Integer, Double> COS_TABLE = Map.ofEntries(
            Map.entry(0, 1.0),
            Map.entry(30, SQRT3_OVER_2), Map.entry(330, SQRT3_OVER_2),
            Map.entry(45, SQRT2_OVER_2), Map.entry(315, SQRT2_OVER_2),
            Map.entry(60, 0.5), Map.entry(300, 0.5),
            Map.entry(90, 0.0), Map.entry(270, 0.0),
            Map.entry(120, -0.5), Map.entry(240, -0.5),
            Map.entry(135, -SQRT2_OVER_2), Map.entry(225, -SQRT2_OVER_2),
            Map.entry(150, -SQRT3_OVER_2), Map.entry(210, -SQRT3_OVER_2),
            Map.entry(180, -1.0));

    private static final Map<Integer, Double> TAN_TABLE = Map.ofEntries(
            Map.entry(0, 0.0), Map.entry(180, 0.0),
            Map.entry(30, ONE_OVER_SQRT3), Map.entry(210, ONE_OVER_SQRT3),
            Map.entry(45, 1.0), Map.entry(225, 1.0),
            Map.entry(60, SQRT3), Map.entry(240, SQRT3),
            Map.entry(120, -SQRT3), Map.entry(300, -SQRT3),
            Map.entry(135, -1.0), Map.entry(315, -1.0),
            Map.entry(150, -ONE_OVER_SQRT3), Map.entry(330, -ONE_OVER_SQRT3));

    @Tool(description = "Compute square root of a number.")
    public String sqrt(
            @ToolParam(description = "Non-negative number")
            final double number) {
        return number < 0
                ? "Error: Square root is undefined for negative numbers. Received: " + number
                : String.valueOf(StrictMath.sqrt(number));
    }

    @Tool(description = "Compute natural logarithm (ln) of a number.")
    public String log(
            @ToolParam(description = "Positive number")
            final double number) {
        return number <= 0
                ? "Error: Natural logarithm is undefined for non-positive numbers. Received: " + number
                : String.valueOf(StrictMath.log(number));
    }

    @Tool(description = "Compute base-10 logarithm of a number.")
    public String log10(
            @ToolParam(description = "Positive number")
            final double number) {
        return number <= 0
                ? "Error: Base-10 logarithm is undefined for non-positive numbers. Received: " + number
                : String.valueOf(StrictMath.log10(number));
    }

    @Tool(description = "Compute factorial (n!). Range: 0 to 20.")
    public String factorial(
            @ToolParam(description = "Integer from 0 to 20")
            final int num) {
        final String result;
        if (num < 0 || num > 20) {
            result = "Error: Factorial is only defined for integers 0 to 20. Received: " + num;
        } else {
            BigInteger value = BigInteger.ONE;
            for (int idx = 2; idx <= num; idx++) {
                value = value.multiply(BigInteger.valueOf(idx));
            }
            result = value.toString();
        }
        return result;
    }

    @Tool(description = "Compute sine of an angle in degrees.")
    public String sin(
            @ToolParam(description = "Angle in degrees") final double degrees) {
        final double exact = isIntegerAngle(degrees)
                ? SIN_TABLE.getOrDefault(normalizeAngle(degrees), Double.NaN)
                : Double.NaN;
        return String.valueOf(Double.isNaN(exact)
                ? StrictMath.sin(StrictMath.toRadians(degrees)) : exact);
    }

    @Tool(description = "Compute cosine of an angle in degrees.")
    public String cos(
            @ToolParam(description = "Angle in degrees") final double degrees) {
        final double exact = isIntegerAngle(degrees)
                ? COS_TABLE.getOrDefault(normalizeAngle(degrees), Double.NaN)
                : Double.NaN;
        return String.valueOf(Double.isNaN(exact)
                ? StrictMath.cos(StrictMath.toRadians(degrees)) : exact);
    }

    @Tool(description = "Compute tangent of an angle in degrees.")
    public String tan(
            @ToolParam(description = "Angle in degrees") final double degrees) {
        final boolean isInteger = isIntegerAngle(degrees);
        final int normalized = isInteger ? normalizeAngle(degrees) : -1;
        final String result;
        if (normalized == 90 || normalized == 270) {
            result = "Error: Tangent is undefined at " + (int) degrees
                    + " degrees (vertical asymptote).";
        } else {
            final double exact = isInteger
                    ? TAN_TABLE.getOrDefault(normalized, Double.NaN)
                    : Double.NaN;
            result = String.valueOf(Double.isNaN(exact)
                    ? StrictMath.tan(StrictMath.toRadians(degrees)) : exact);
        }
        return result;
    }

    private static boolean isIntegerAngle(final double degrees) {
        return degrees == StrictMath.floor(degrees);
    }

    private static int normalizeAngle(final double degrees) {
        int angle = ((int) degrees) % FULL_CIRCLE;
        if (angle < 0) {
            angle += FULL_CIRCLE;
        }
        return angle;
    }
}
