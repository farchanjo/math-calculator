package com.archanjo.mathcalculator.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.ExpressionEvaluator;

@Component
public class GraphingCalculatorTool {

    private static final int MAX_NEWTON_ITERS = 1000;
    private static final double NEWTON_TOLERANCE = 1e-12;
    private static final double DERIVATIVE_STEP = 1e-8;
    private static final int BISECT_ITERS = 50;
    private static final int SCAN_DIVISIONS = 1000;
    @Tool(description = "Plot a function. Returns JSON array of {x, y} points.")
    public String plotFunction(
            @ToolParam(description = "Expression, e.g. 'x^2'")
            final String expression,
            @ToolParam(description = "Variable name, e.g. 'x'")
            final String variable,
            @ToolParam(description = "Range minimum")
            final double min,
            @ToolParam(description = "Range maximum")
            final double max,
            @ToolParam(description = "Number of points")
            final int steps) {
        if (steps <= 0) {
            throw new IllegalArgumentException("Steps must be greater than 0");
        }
        if (min >= max) {
            throw new IllegalArgumentException("Min must be less than max");
        }

        final double stepSize = (max - min) / steps;
        final StringBuilder builder = new StringBuilder("[");

        for (int idx = 0; idx <= steps; idx++) {
            final double xValue = min + idx * stepSize;
            final double yValue = ExpressionEvaluator.evaluate(
                    expression, Map.of(variable, xValue));

            if (idx > 0) {
                builder.append(", ");
            }
            builder.append("{\"x\":").append(xValue)
                   .append(",\"y\":").append(yValue)
                   .append('}');
        }

        builder.append(']');
        return builder.toString();
    }

    @Tool(description = "Solve f(x)=0 via Newton-Raphson. Returns root value.")
    public String solveEquation(
            @ToolParam(description = "Expression, e.g. 'x^2 - 4'")
            final String expression,
            @ToolParam(description = "Variable name, e.g. 'x'")
            final String variable,
            @ToolParam(description = "Initial guess for the root")
            final double initialGuess) {
        double guess = initialGuess;

        for (int idx = 0; idx < MAX_NEWTON_ITERS; idx++) {
            final double functionValue = ExpressionEvaluator.evaluate(
                    expression, Map.of(variable, guess));

            if (StrictMath.abs(functionValue) < NEWTON_TOLERANCE) {
                return String.valueOf(guess);
            }

            final double fxPlusH = ExpressionEvaluator.evaluate(
                    expression, Map.of(variable, guess + DERIVATIVE_STEP));
            final double fxMinusH = ExpressionEvaluator.evaluate(
                    expression,
                    Map.of(variable, guess - DERIVATIVE_STEP));
            final double derivative =
                    (fxPlusH - fxMinusH) / (2 * DERIVATIVE_STEP);

            if (derivative == 0) {
                throw new IllegalArgumentException(
                        "Derivative is zero; Newton-Raphson cannot continue");
            }

            guess = guess - functionValue / derivative;
        }

        throw new IllegalArgumentException(
                "Failed to converge after 1000 iterations");
    }

    @Tool(description = "Find all real roots of f(x)=0 in an interval.")
    public String findRoots(
            @ToolParam(description = "Expression, e.g. 'x^3 - x'")
            final String expression,
            @ToolParam(description = "Variable name, e.g. 'x'")
            final String variable,
            @ToolParam(description = "Interval lower bound")
            final double min,
            @ToolParam(description = "Interval upper bound")
            final double max) {
        final double step = (max - min) / SCAN_DIVISIONS;
        final List<Double> roots = new ArrayList<>();

        double prevX = min;
        double prevF = ExpressionEvaluator.evaluate(
                expression, Map.of(variable, prevX));

        if (StrictMath.abs(prevF) < NEWTON_TOLERANCE) {
            roots.add(prevX);
        }

        for (int idx = 1; idx <= SCAN_DIVISIONS; idx++) {
            final double currentX = min + idx * step;
            final double currentF = ExpressionEvaluator.evaluate(
                    expression, Map.of(variable, currentX));

            if (StrictMath.abs(currentF) < NEWTON_TOLERANCE) {
                roots.add(currentX);
            } else if (prevF * currentF < 0) {
                final double root = bisect(
                        expression, variable, prevX, currentX);
                roots.add(root);
            }

            prevX = currentX;
            prevF = currentF;
        }

        final StringBuilder builder = new StringBuilder("[");
        for (int idx = 0; idx < roots.size(); idx++) {
            if (idx > 0) {
                builder.append(", ");
            }
            builder.append(roots.get(idx));
        }
        builder.append(']');
        return builder.toString();
    }

    private double bisect(final String expression, final String variable,
                          final double lowerBound, final double upperBound) {
        double lower = lowerBound;
        double upper = upperBound;
        for (int idx = 0; idx < BISECT_ITERS; idx++) {
            final double mid = (lower + upper) / 2;
            final double fMid = ExpressionEvaluator.evaluate(
                    expression, Map.of(variable, mid));

            if (StrictMath.abs(fMid) < NEWTON_TOLERANCE) {
                break;
            }

            final double fLo = ExpressionEvaluator.evaluate(
                    expression, Map.of(variable, lower));
            if (fLo * fMid < 0) {
                upper = mid;
            } else {
                lower = mid;
            }
        }
        return (lower + upper) / 2;
    }
}
