package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GraphingCalculatorToolTest {

    private static final double DELTA = 1e-6;
    private static final String X_SQUARED = "x^2";
    private static final String X_SQUARED_MINUS_4 = "x^2 - 4";

    private final GraphingCalculatorTool tool = new GraphingCalculatorTool();

    @Nested
    @DisplayName("plotFunction")
    class PlotFunction {

        @Test
        void returnsJsonArrayStartingWithBracket() {
            final String result = tool.plotFunction(X_SQUARED, "x", 0, 2, 2);

            assertTrue(result.startsWith("["), "Result should start with '['");
        }

        @Test
        void returnsJsonArrayEndingWithBracket() {
            final String result = tool.plotFunction(X_SQUARED, "x", 0, 2, 2);

            assertTrue(result.endsWith("]"), "Result should end with ']'");
        }

        @Test
        void returnsResultContainingXKey() {
            final String result = tool.plotFunction(X_SQUARED, "x", 0, 2, 2);

            assertTrue(result.contains("x"), "Result should contain 'x' key");
        }

        @Test
        void returnsResultContainingYKey() {
            final String result = tool.plotFunction(X_SQUARED, "x", 0, 2, 2);

            assertTrue(result.contains("y"), "Result should contain 'y' key");
        }

        @Test
        void throwsWhenStepsIsZero() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.plotFunction(X_SQUARED, "x", 0, 2, 0),
                    "Should throw when steps is zero");
        }

        @Test
        void throwsWhenStepsIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.plotFunction(X_SQUARED, "x", 0, 2, -1),
                    "Should throw when steps is negative");
        }

        @Test
        void throwsWhenMinEqualsMax() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.plotFunction(X_SQUARED, "x", 2, 2, 2),
                    "Should throw when min equals max");
        }

        @Test
        void throwsWhenMinIsGreaterThanMax() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.plotFunction(X_SQUARED, "x", 3, 1, 2),
                    "Should throw when min is greater than max");
        }
    }

    @Nested
    @DisplayName("solveEquation")
    class SolveEquation {

        @Test
        void findsPositiveRoot() {
            final String result = tool.solveEquation(X_SQUARED_MINUS_4, "x", 3.0);

            final double root = Double.parseDouble(result);
            assertEquals(2.0, root, DELTA, "Root should be close to 2.0");
        }

        @Test
        void findsNegativeRoot() {
            final String result = tool.solveEquation(X_SQUARED_MINUS_4, "x", -3.0);

            final double root = Double.parseDouble(result);
            assertEquals(-2.0, root, DELTA, "Root should be close to -2.0");
        }
    }

    @Nested
    @DisplayName("findRoots")
    class FindRoots {

        @Test
        void returnsJsonArrayStartingWithBracket() {
            final String result = tool.findRoots(X_SQUARED_MINUS_4, "x", -5, 5);

            assertTrue(result.startsWith("["), "Result should be a JSON array starting with '['");
        }

        @Test
        void returnsJsonArrayEndingWithBracket() {
            final String result = tool.findRoots(X_SQUARED_MINUS_4, "x", -5, 5);

            assertTrue(result.endsWith("]"), "Result should be a JSON array ending with ']'");
        }

        @Test
        void containsNegativeRootForQuadratic() {
            final String result = tool.findRoots(X_SQUARED_MINUS_4, "x", -5, 5);

            assertTrue(result.contains("-2") || result.contains("-2.0"),
                    "Result should contain a root close to -2");
        }

        @Test
        void containsPositiveRootForQuadratic() {
            final String result = tool.findRoots(X_SQUARED_MINUS_4, "x", -5, 5);

            assertTrue(result.contains("2") || result.contains("2.0"),
                    "Result should contain a root close to 2");
        }

        @Test
        void returnsEmptyArrayWhenNoRealRoots() {
            final String result = tool.findRoots("x^2 + 1", "x", -5, 5);

            assertEquals("[]", result, "Should return empty array when no real roots exist");
        }
    }
}
