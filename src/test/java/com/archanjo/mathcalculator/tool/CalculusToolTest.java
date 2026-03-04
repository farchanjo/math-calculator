package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculusToolTest {

    private static final double TOLERANCE = 0.01;
    private static final String EXPR_X2 = "x^2";
    private static final String EXPR_X3 = "x^3";
    private static final String VAR_X = "x";

    private final CalculusTool tool = new CalculusTool();

    private void assertClose(
            final double expected, final double actual,
            final String msg) {
        assertEquals(true,
                StrictMath.abs(expected - actual) < TOLERANCE,
                msg + " — expected: " + expected
                        + ", actual: " + actual);
    }

    @Nested
    @DisplayName("derivative")
    class DerivativeTests {

        @Test
        void derivativeOfXSquaredAt3() {
            final String result = tool.derivative(EXPR_X2, VAR_X, 3.0);
            assertClose(6.0, Double.parseDouble(result),
                    "d/dx(x^2) at x=3 should be 6");
        }

        @Test
        void derivativeOfXCubedAt2() {
            final String result = tool.derivative(EXPR_X3, VAR_X, 2.0);
            assertClose(12.0, Double.parseDouble(result),
                    "d/dx(x^3) at x=2 should be 12");
        }

        @Test
        void derivativeOfConstant() {
            final String result = tool.derivative("5", VAR_X, 1.0);
            assertClose(0.0, Double.parseDouble(result),
                    "d/dx(5) should be 0");
        }

        @Test
        void derivativeOfLinear() {
            final String result = tool.derivative("3*x + 2", VAR_X, 7.0);
            assertClose(3.0, Double.parseDouble(result),
                    "d/dx(3x+2) should be 3");
        }

        @Test
        void derivativeOfSinAtZero() {
            final String result = tool.derivative("sin(x)", VAR_X, 0.0);
            final double expected = StrictMath.cos(
                    StrictMath.toRadians(0.0))
                    * StrictMath.PI / 180.0;
            assertClose(expected, Double.parseDouble(result),
                    "d/dx(sin(x)) at x=0 should be cos(0)*pi/180");
        }
    }

    @Nested
    @DisplayName("nthDerivative")
    class NthDerivativeTests {

        @Test
        void firstDerivativeOfXCubed() {
            final String result = tool.nthDerivative(
                    EXPR_X3, VAR_X, 2.0, 1);
            assertClose(12.0, Double.parseDouble(result),
                    "f'(x^3) at x=2 should be 12");
        }

        @Test
        void secondDerivativeOfXCubed() {
            final String result = tool.nthDerivative(
                    EXPR_X3, VAR_X, 2.0, 2);
            assertClose(12.0, Double.parseDouble(result),
                    "f''(x^3) at x=2 should be 12");
        }

        @Test
        void thirdDerivativeOfXCubed() {
            final String result = tool.nthDerivative(
                    EXPR_X3, VAR_X, 2.0, 3);
            assertClose(6.0, Double.parseDouble(result),
                    "f'''(x^3) at x=2 should be 6");
        }

        @Test
        void invalidOrderReturnsError() {
            final String result = tool.nthDerivative(
                    EXPR_X2, VAR_X, 1.0, 0);
            assertTrue(result.startsWith("Error:"),
                    "Order 0 should return error");
        }

        @Test
        void orderAboveMaxReturnsError() {
            final String result = tool.nthDerivative(
                    EXPR_X2, VAR_X, 1.0, 11);
            assertTrue(result.startsWith("Error:"),
                    "Order 11 should return error");
        }
    }

    @Nested
    @DisplayName("definiteIntegral")
    class DefiniteIntegralTests {

        @Test
        void integralOfXSquaredFrom0To1() {
            final String result = tool.definiteIntegral(
                    EXPR_X2, VAR_X, 0.0, 1.0);
            assertClose(1.0 / 3.0, Double.parseDouble(result),
                    "integral of x^2 from 0 to 1 = 1/3");
        }

        @Test
        void integralOfXFrom0To2() {
            final String result = tool.definiteIntegral(
                    VAR_X, VAR_X, 0.0, 2.0);
            assertClose(2.0, Double.parseDouble(result),
                    "integral of x from 0 to 2 = 2");
        }

        @Test
        void integralOfConstant() {
            final String result = tool.definiteIntegral(
                    "3", VAR_X, 1.0, 5.0);
            assertClose(12.0, Double.parseDouble(result),
                    "integral of 3 from 1 to 5 = 12");
        }

        @Test
        void integralOfXCubedFrom0To2() {
            final String result = tool.definiteIntegral(
                    EXPR_X3, VAR_X, 0.0, 2.0);
            assertClose(4.0, Double.parseDouble(result),
                    "integral of x^3 from 0 to 2 = 4");
        }

        @Test
        void reverseIntegralBounds() {
            final String result = tool.definiteIntegral(
                    EXPR_X2, VAR_X, 1.0, 0.0);
            assertClose(-1.0 / 3.0, Double.parseDouble(result),
                    "integral with reversed bounds should be negative");
        }
    }

    @Nested
    @DisplayName("tangentLine")
    class TangentLineTests {

        @Test
        void tangentToXSquaredAt2HasSlope() {
            final String result = tool.tangentLine(
                    EXPR_X2, VAR_X, 2.0);
            assertTrue(result.contains("\"slope\""),
                    "Should return JSON with slope");
        }

        @Test
        void tangentToXSquaredAt2HasYIntercept() {
            final String result = tool.tangentLine(
                    EXPR_X2, VAR_X, 2.0);
            assertTrue(result.contains("\"yIntercept\""),
                    "Should return JSON with yIntercept");
        }

        @Test
        void tangentToXSquaredAt2HasEquation() {
            final String result = tool.tangentLine(
                    EXPR_X2, VAR_X, 2.0);
            assertTrue(result.contains("\"equation\""),
                    "Should return JSON with equation");
        }

        @Test
        void tangentSlopeIsDerivative() {
            final String result = tool.tangentLine(
                    EXPR_X2, VAR_X, 3.0);
            assertTrue(result.contains("6.0"),
                    "Slope at x=3 for x^2 should be ~6.0");
        }
    }
}
