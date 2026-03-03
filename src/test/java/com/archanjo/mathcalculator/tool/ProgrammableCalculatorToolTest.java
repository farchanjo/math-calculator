package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProgrammableCalculatorToolTest {

    private static final String VAR_X_3_Y_1 = "{\"x\": 3.0, \"y\": 1.0}";
    private static final String VAR_X_5 = "{\"x\": 5.0}";
    private static final String VAR_X_1 = "{\"x\": 1.0}";

    private final ProgrammableCalculatorTool tool = new ProgrammableCalculatorTool();
    @Nested
    @DisplayName("evaluate - basic arithmetic")
    class BasicArithmetic {
        @Test
        void simpleAddition() {
            assertEquals("5.0", tool.evaluate("2 + 3"),
                    "2 + 3 should equal 5.0");
        }

        @Test
        void operatorPrecedence() {
            assertEquals("10.0", tool.evaluate("2 * 3 + 4"),
                    "2 * 3 + 4 should equal 10.0");
        }

        @Test
        void exponentiation() {
            assertEquals("8.0", tool.evaluate("2 ^ 3"),
                    "2 ^ 3 should equal 8.0");
        }

        @Test
        void parentheses() {
            assertEquals("20.0", tool.evaluate("(2 + 3) * 4"),
                    "(2 + 3) * 4 should equal 20.0");
        }
    }

    @Nested
    @DisplayName("evaluate - functions")
    class Functions {
        @Test
        void sqrtFunction() {
            assertEquals("4.0", tool.evaluate("sqrt(16)"),
                    "sqrt(16) should equal 4.0");
        }
    }

    @Nested
    @DisplayName("evaluate - invalid input")
    class InvalidInput {
        @Test
        void nullExpressionThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.evaluate(null),
                    "Null expression should throw IllegalArgumentException");
        }

        @Test
        void emptyExpressionThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.evaluate(""),
                    "Empty expression should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("evaluateWithVariables - valid expressions")
    class WithVariables {
        @Test
        void multipleVariables() {
            assertEquals("7.0",
                    tool.evaluateWithVariables("2*x + y", VAR_X_3_Y_1),
                    "2*x + y with x=3, y=1 should equal 7.0");
        }

        @Test
        void variableExponent() {
            assertEquals("25.0",
                    tool.evaluateWithVariables("x^2", VAR_X_5),
                    "x^2 with x=5 should equal 25.0");
        }
    }

    @Nested
    @DisplayName("evaluateWithVariables - invalid input")
    class WithVariablesInvalid {
        @Test
        void nullExpressionThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.evaluateWithVariables(null, VAR_X_1),
                    "Null expression should throw IllegalArgumentException");
        }

        @Test
        void nullVariablesThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.evaluateWithVariables("x + 1", null),
                    "Null variables should throw IllegalArgumentException");
        }
    }
}
