package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScientificCalculatorToolTest {

    private static final String ZERO = "0.0";
    private static final String ONE = "1.0";
    private static final String HALF = "0.5";
    private static final String ERROR_PREFIX = "Error:";

    private final ScientificCalculatorTool tool = new ScientificCalculatorTool();

    @Nested
    @DisplayName("sqrt")
    class SqrtTests {

        @Test
        void sqrtOfFourReturnsTwo() {
            assertEquals("2.0", tool.sqrt(4),
                "sqrt(4) should return '2.0'");
        }

        @Test
        void sqrtOfZeroReturnsZero() {
            assertEquals(ZERO, tool.sqrt(0),
                "sqrt(0) should return '0.0'");
        }

        @Test
        void sqrtOfNegativeReturnsError() {
            assertTrue(tool.sqrt(-1).startsWith(ERROR_PREFIX),
                "sqrt(-1) should return an error message");
        }
    }

    @Nested
    @DisplayName("log (natural logarithm)")
    class LogTests {

        @Test
        void logOfEReturnsOne() {
            assertEquals(ONE, tool.log(StrictMath.E),
                "log(e) should return '1.0'");
        }

        @Test
        void logOfZeroReturnsError() {
            assertTrue(tool.log(0).startsWith(ERROR_PREFIX),
                "log(0) should return an error message");
        }

        @Test
        void logOfNegativeReturnsError() {
            assertTrue(tool.log(-1).startsWith(ERROR_PREFIX),
                "log(-1) should return an error message");
        }
    }

    @Nested
    @DisplayName("log10")
    class Log10Tests {

        @Test
        void log10Of100ReturnsTwo() {
            assertEquals("2.0", tool.log10(100), "log10(100) should return '2.0'");
        }

        @Test
        void log10OfZeroReturnsError() {
            assertTrue(tool.log10(0).startsWith(ERROR_PREFIX),
                "log10(0) should return an error message");
        }
    }

    @Nested
    @DisplayName("factorial")
    class FactorialTests {

        @Test
        void factorialOfZeroReturnsOne() {
            assertEquals("1", tool.factorial(0),
                "factorial(0) should return '1'");
        }

        @Test
        void factorialOfFiveReturns120() {
            assertEquals("120", tool.factorial(5),
                "factorial(5) should return '120'");
        }

        @Test
        void factorialOf20ReturnsCorrectValue() {
            assertEquals("2432902008176640000", tool.factorial(20),
                "factorial(20) should return 2432902008176640000");
        }

        @Test
        void factorialOfNegativeReturnsError() {
            assertTrue(tool.factorial(-1).startsWith(ERROR_PREFIX),
                "factorial(-1) should return an error message");
        }

        @Test
        void factorialAbove20ReturnsError() {
            assertTrue(tool.factorial(21).startsWith(ERROR_PREFIX),
                "factorial(21) should return an error message");
        }
    }

    @Nested
    @DisplayName("sin")
    class SinTests {

        @Test
        void sinOfZeroReturnsZero() {
            assertEquals(ZERO, tool.sin(0),
                "sin(0) should return '0.0'");
        }

        @Test
        void sinOf90ReturnsOne() {
            assertEquals(ONE, tool.sin(90),
                "sin(90) should return '1.0'");
        }

        @Test
        void sinOf30ReturnsHalf() {
            assertEquals(HALF, tool.sin(30),
                "sin(30) should return exactly '0.5'");
        }

        @Test
        void sinOf360ReturnsZero() {
            assertEquals(ZERO, tool.sin(360),
                "sin(360) should return exactly '0.0'");
        }

        @Test
        void sinOfNegative30ReturnsNegativeHalf() {
            assertEquals("-0.5", tool.sin(-30),
                "sin(-30) should return exactly '-0.5'");
        }

        @Test
        void sinOf390ReturnsHalf() {
            assertEquals(HALF, tool.sin(390),
                "sin(390) should return exactly '0.5'");
        }
    }

    @Nested
    @DisplayName("cos")
    class CosTests {

        @Test
        void cosOfZeroReturnsOne() {
            assertEquals(ONE, tool.cos(0),
                "cos(0) should return '1.0'");
        }

        @Test
        void cosOf90ReturnsZero() {
            assertEquals(ZERO, tool.cos(90),
                "cos(90) should return '0.0'");
        }

        @Test
        void cosOf60ReturnsHalf() {
            assertEquals(HALF, tool.cos(60),
                "cos(60) should return exactly '0.5'");
        }

        @Test
        void cosOfNegative60ReturnsHalf() {
            assertEquals(HALF, tool.cos(-60),
                "cos(-60) should return exactly '0.5'");
        }
    }

    @Nested
    @DisplayName("tan")
    class TanTests {

        @Test
        void tanOfZeroReturnsZero() {
            assertEquals(ZERO, tool.tan(0),
                "tan(0) should return '0.0'");
        }

        @Test
        void tanOf45ReturnsOne() {
            assertEquals(ONE, tool.tan(45),
                "tan(45) should return exactly '1.0'");
        }

        @Test
        void tanOf90ReturnsError() {
            assertTrue(tool.tan(90).startsWith(ERROR_PREFIX),
                "tan(90) should return an error — tangent is undefined");
        }

        @Test
        void tanOf270ReturnsError() {
            assertTrue(tool.tan(270).startsWith(ERROR_PREFIX),
                "tan(270) should return an error — tangent is undefined");
        }

        @Test
        void tanOfNegative90ReturnsError() {
            assertTrue(tool.tan(-90).startsWith(ERROR_PREFIX),
                "tan(-90) should return an error — tangent is undefined");
        }
    }
}
