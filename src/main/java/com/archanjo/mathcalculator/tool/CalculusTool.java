package com.archanjo.mathcalculator.tool;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.ExpressionEvaluator;

/**
 * MCP tools for numerical calculus: derivatives and integrals.
 *
 * <p>Uses the built-in {@link ExpressionEvaluator} for function evaluation,
 * five-point central difference for derivatives, and composite Simpson's rule
 * for definite integrals.
 */
@Component
public class CalculusTool {

    private static final double DERIVATIVE_STEP = 1e-6;
    private static final int SIMPSON_INTERVALS = 10_000;
    private static final int MAX_ORDER = 10;
    private static final String ERROR_PREFIX = "Error: ";
    private static final String VAR_DESC = "Variable name, e.g. 'x'";

    @Tool(description = "Compute numerical derivative of f(x) at a point"
            + " using five-point central difference.")
    public String derivative(
            @ToolParam(description = "Expression, e.g. 'x^2 + 3*x'")
            final String expression,
            @ToolParam(description = VAR_DESC)
            final String variable,
            @ToolParam(description = "Point at which to evaluate the derivative")
            final double point) {

        String result;
        try {
            final double value = centralDifference(
                    expression, variable, point, DERIVATIVE_STEP);
            result = String.valueOf(value);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute nth-order numerical derivative of f(x)"
            + " at a point. Supports orders 1 to 10.")
    public String nthDerivative(
            @ToolParam(description = "Expression, e.g. 'x^3'")
            final String expression,
            @ToolParam(description = VAR_DESC)
            final String variable,
            @ToolParam(description = "Point at which to evaluate")
            final double point,
            @ToolParam(description = "Derivative order (1 to 10)")
            final int order) {

        String result;
        try {
            validateOrder(order);
            final double value = nthDeriv(
                    expression, variable, point, order);
            result = String.valueOf(value);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute definite integral of f(x) from lower to upper"
            + " bound using composite Simpson's rule.")
    public String definiteIntegral(
            @ToolParam(description = "Expression, e.g. 'x^2'")
            final String expression,
            @ToolParam(description = VAR_DESC)
            final String variable,
            @ToolParam(description = "Lower bound of integration")
            final double lower,
            @ToolParam(description = "Upper bound of integration")
            final double upper) {

        String result;
        try {
            final double value = simpson(
                    expression, variable, lower, upper);
            result = String.valueOf(value);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute tangent line to f(x) at a point."
            + " Returns JSON with slope, yIntercept, and equation.")
    public String tangentLine(
            @ToolParam(description = "Expression, e.g. 'x^2'")
            final String expression,
            @ToolParam(description = VAR_DESC)
            final String variable,
            @ToolParam(description = "Point of tangency")
            final double point) {

        String result;
        try {
            final double fAtPoint = eval(expression, variable, point);
            final double slope = centralDifference(
                    expression, variable, point, DERIVATIVE_STEP);
            final double yIntercept = fAtPoint - slope * point;
            result = "{\"slope\":%s,\"yIntercept\":%s,\"equation\":\"y = %s*x + %s\"}"
                    .formatted(slope, yIntercept, slope, yIntercept);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private double centralDifference(
            final String expression, final String variable,
            final double point, final double step) {

        final double fMinus2 = eval(expression, variable, point - 2 * step);
        final double fMinus1 = eval(expression, variable, point - step);
        final double fPlus1 = eval(expression, variable, point + step);
        final double fPlus2 = eval(expression, variable, point + 2 * step);
        return (-fPlus2 + 8 * fPlus1 - 8 * fMinus1 + fMinus2) / (12 * step);
    }

    private double nthDeriv(
            final String expression, final String variable,
            final double point, final int order) {

        final double step = StrictMath.pow(DERIVATIVE_STEP, 1.0 / order) * 10;
        final int halfN = order / 2;
        double result = 0;

        for (int idx = 0; idx <= order; idx++) {
            final double xSample = point + (idx - halfN) * step;
            final double fSample = eval(expression, variable, xSample);
            final double coeff = binomialCoeff(order, idx)
                    * StrictMath.pow(-1, order - idx);
            result += coeff * fSample;
        }
        return result / StrictMath.pow(step, order);
    }

    private double binomialCoeff(final int total, final int choose) {
        double result = 1;
        final int bound = StrictMath.min(choose, total - choose);
        for (int idx = 0; idx < bound; idx++) {
            result = result * (total - idx) / (idx + 1);
        }
        return result;
    }

    private double simpson(
            final String expression, final String variable,
            final double lower, final double upper) {

        final int intervals = SIMPSON_INTERVALS;
        final double step = (upper - lower) / intervals;
        double sum = eval(expression, variable, lower)
                + eval(expression, variable, upper);

        for (int idx = 1; idx < intervals; idx++) {
            final double xVal = lower + idx * step;
            final double fVal = eval(expression, variable, xVal);
            final int multiplier = (idx % 2 == 0) ? 2 : 4;
            sum += multiplier * fVal;
        }
        return sum * step / 3.0;
    }

    private double eval(
            final String expression, final String variable,
            final double value) {

        return ExpressionEvaluator.evaluate(
                expression, Map.of(variable, value));
    }

    private void validateOrder(final int order) {
        if (order < 1 || order > MAX_ORDER) {
            throw new IllegalArgumentException(
                    "Order must be between 1 and " + MAX_ORDER
                            + ". Received: " + order);
        }
    }
}
