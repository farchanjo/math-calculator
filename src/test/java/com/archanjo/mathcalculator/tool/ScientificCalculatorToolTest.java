package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScientificCalculatorToolTest {

    private static final double DELTA = 1e-10;

    private final ScientificCalculatorTool tool = new ScientificCalculatorTool();

    @Nested
    @DisplayName("sqrt")
    class SqrtTests {

        @Test
        void sqrtOfFourReturnsTwo() {
            assertEquals(2.0, tool.sqrt(4), DELTA,
                "sqrt(4) should return 2.0");
        }

        @Test
        void sqrtOfZeroReturnsZero() {
            assertEquals(0.0, tool.sqrt(0), DELTA,
                "sqrt(0) should return 0.0");
        }

        @Test
        void sqrtOfNegativeThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> tool.sqrt(-1),
                "sqrt of negative number should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("log (natural logarithm)")
    class LogTests {

        @Test
        void logOfEReturnsOne() {
            assertEquals(1.0, tool.log(StrictMath.E), DELTA,
                "log(e) should return 1.0");
        }

        @Test
        void logOfZeroThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> tool.log(0),
                "log(0) should throw IllegalArgumentException");
        }

        @Test
        void logOfNegativeThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> tool.log(-1),
                "log of negative number should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("log10")
    class Log10Tests {

        @Test
        void log10Of100ReturnsTwo() {
            assertEquals(2.0, tool.log10(100), DELTA,
                "log10(100) should return 2.0");
        }

        @Test
        void log10OfZeroThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> tool.log10(0),
                "log10(0) should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("factorial")
    class FactorialTests {

        @Test
        void factorialOfZeroReturnsOne() {
            assertEquals("1", tool.factorial(0),
                "factorial(0) should return 1");
        }

        @Test
        void factorialOfFiveReturns120() {
            assertEquals("120", tool.factorial(5),
                "factorial(5) should return 120");
        }

        @Test
        void factorialOf20ReturnsCorrectValue() {
            assertEquals("2432902008176640000", tool.factorial(20),
                "factorial(20) should return 2432902008176640000");
        }

        @Test
        void factorialOfNegativeThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> tool.factorial(-1),
                "factorial of negative should throw IllegalArgumentException");
        }

        @Test
        void factorialAbove20Throws() {
            assertThrows(IllegalArgumentException.class,
                () -> tool.factorial(21),
                "factorial above 20 should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("sin")
    class SinTests {

        @Test
        void sinOfZeroReturnsZero() {
            assertEquals(0.0, tool.sin(0), DELTA,
                "sin(0) should return 0.0");
        }

        @Test
        void sinOf90ReturnsOne() {
            assertEquals(1.0, tool.sin(90), DELTA,
                "sin(90) should return 1.0");
        }

        @Test
        void sinOf30ReturnsHalf() {
            assertEquals(0.5, tool.sin(30), DELTA,
                "sin(30) should return 0.5");
        }
    }

    @Nested
    @DisplayName("cos")
    class CosTests {

        @Test
        void cosOfZeroReturnsOne() {
            assertEquals(1.0, tool.cos(0), DELTA,
                "cos(0) should return 1.0");
        }

        @Test
        void cosOf90ReturnsZero() {
            assertEquals(0.0, tool.cos(90), DELTA,
                "cos(90) should return 0.0");
        }

        @Test
        void cosOf60ReturnsHalf() {
            assertEquals(0.5, tool.cos(60), DELTA,
                "cos(60) should return 0.5");
        }
    }

    @Nested
    @DisplayName("tan")
    class TanTests {

        @Test
        void tanOfZeroReturnsZero() {
            assertEquals(0.0, tool.tan(0), DELTA,
                "tan(0) should return 0.0");
        }

        @Test
        void tanOf45ReturnsOne() {
            assertEquals(1.0, tool.tan(45), DELTA,
                "tan(45) should return 1.0");
        }
    }
}
