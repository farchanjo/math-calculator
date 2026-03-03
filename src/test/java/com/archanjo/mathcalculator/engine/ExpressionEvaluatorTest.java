package com.archanjo.mathcalculator.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionEvaluatorTest {

    private static final double DELTA = 1e-10;
    // ------------------------------------------------------------------ //
    //  Basic arithmetic
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Basic arithmetic")
    class BasicArithmetic {
        @Test
        @DisplayName("addition: 2+3 = 5")
        void addition() {
            assertEquals(5.0, ExpressionEvaluator.evaluate("2+3"), DELTA,
                    "2+3 should equal 5.0");
        }

        @Test
        @DisplayName("subtraction: 10-3 = 7")
        void subtraction() {
            assertEquals(7.0, ExpressionEvaluator.evaluate("10-3"), DELTA,
                    "10-3 should equal 7.0");
        }

        @Test
        @DisplayName("multiplication: 3*4 = 12")
        void multiplication() {
            assertEquals(12.0, ExpressionEvaluator.evaluate("3*4"), DELTA,
                    "3*4 should equal 12.0");
        }

        @Test
        @DisplayName("division: 10/3 = 3.333...")
        void division() {
            assertEquals(10.0 / 3.0, ExpressionEvaluator.evaluate("10/3"),
                    DELTA, "10/3 should equal 3.333...");
        }
    }

    // ------------------------------------------------------------------ //
    //  Operator precedence and parentheses
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Operator precedence and parentheses")
    class PrecedenceAndParens {
        @Test
        @DisplayName("precedence: 2+3*4 = 14")
        void operatorPrecedence() {
            assertEquals(14.0, ExpressionEvaluator.evaluate("2+3*4"), DELTA,
                    "2+3*4 should equal 14.0 due to operator precedence");
        }

        @Test
        @DisplayName("parentheses: (2+3)*4 = 20")
        void parenthesesOverride() {
            assertEquals(20.0, ExpressionEvaluator.evaluate("(2+3)*4"), DELTA,
                    "(2+3)*4 should equal 20.0");
        }
    }

    // ------------------------------------------------------------------ //
    //  Power operator (right-associative)
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Power operator")
    class PowerOperator {
        @Test
        @DisplayName("simple power: 2^3 = 8")
        void simplePower() {
            assertEquals(8.0, ExpressionEvaluator.evaluate("2^3"), DELTA,
                    "2^3 should equal 8.0");
        }

        @Test
        @DisplayName("right-associative: 2^3^2 = 2^(3^2) = 512")
        void rightAssociative() {
            assertEquals(512.0, ExpressionEvaluator.evaluate("2^3^2"), DELTA,
                    "2^3^2 should equal 512.0 (right-associative)");
        }
    }

    // ------------------------------------------------------------------ //
    //  Modulo and scientific notation
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Modulo and scientific notation")
    class ModuloAndScientific {
        @Test
        @DisplayName("modulo: 10%3 = 1")
        void modulo() {
            assertEquals(1.0, ExpressionEvaluator.evaluate("10%3"), DELTA,
                    "10%3 should equal 1.0");
        }

        @Test
        @DisplayName("scientific notation: 1.5e2 = 150")
        void scientificNotation() {
            assertEquals(150.0, ExpressionEvaluator.evaluate("1.5e2"), DELTA,
                    "1.5e2 should equal 150.0");
        }
    }

    // ------------------------------------------------------------------ //
    //  Unary negation
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Unary negation")
    class UnaryNegation {
        @Test
        @DisplayName("negative literal: -5 = -5")
        void negativeLiteral() {
            assertEquals(-5.0, ExpressionEvaluator.evaluate("-5"), DELTA,
                    "-5 should evaluate to -5.0");
        }

        @Test
        @DisplayName("negated group: -(3+2) = -5")
        void negatedGroup() {
            assertEquals(-5.0, ExpressionEvaluator.evaluate("-(3+2)"), DELTA,
                    "-(3+2) should evaluate to -5.0");
        }
    }

    // ------------------------------------------------------------------ //
    //  Built-in functions
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Built-in functions")
    class BuiltInFunctions {
        @Test
        @DisplayName("sqrt(16) = 4")
        void sqrt() {
            assertEquals(4.0, ExpressionEvaluator.evaluate("sqrt(16)"), DELTA,
                    "sqrt(16) should equal 4.0");
        }

        @Test
        @DisplayName("abs(-5) = 5")
        void abs() {
            assertEquals(5.0, ExpressionEvaluator.evaluate("abs(-5)"), DELTA,
                    "abs(-5) should equal 5.0");
        }

        @Test
        @DisplayName("sin(90) = 1 (degrees)")
        void sinDegrees() {
            assertEquals(1.0, ExpressionEvaluator.evaluate("sin(90)"), DELTA,
                    "sin(90) should equal 1.0 in degrees");
        }

        @Test
        @DisplayName("cos(0) = 1 (degrees)")
        void cosDegrees() {
            assertEquals(1.0, ExpressionEvaluator.evaluate("cos(0)"), DELTA,
                    "cos(0) should equal 1.0 in degrees");
        }

        @Test
        @DisplayName("ceil(1.2) = 2")
        void ceil() {
            assertEquals(2.0, ExpressionEvaluator.evaluate("ceil(1.2)"), DELTA,
                    "ceil(1.2) should equal 2.0");
        }

        @Test
        @DisplayName("floor(1.8) = 1")
        void floor() {
            assertEquals(1.0, ExpressionEvaluator.evaluate("floor(1.8)"),
                    DELTA, "floor(1.8) should equal 1.0");
        }

        @Test
        @DisplayName("log(1) = 0 (natural log)")
        void naturalLog() {
            assertEquals(0.0, ExpressionEvaluator.evaluate("log(1)"), DELTA,
                    "log(1) should equal 0.0");
        }

        @Test
        @DisplayName("log10(100) = 2")
        void log10() {
            assertEquals(2.0, ExpressionEvaluator.evaluate("log10(100)"),
                    DELTA, "log10(100) should equal 2.0");
        }
    }

    // ------------------------------------------------------------------ //
    //  Variables
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Variable substitution")
    class Variables {
        @Test
        @DisplayName("single variable: x+1 with x=5 => 6")
        void singleVariable() {
            final double result = ExpressionEvaluator.evaluate(
                    "x+1", Map.of("x", 5.0));
            assertEquals(6.0, result, DELTA,
                    "x+1 with x=5 should equal 6.0");
        }

        @Test
        @DisplayName("multiple variables: x*y with x=3, y=4 => 12")
        void multipleVariables() {
            final double result = ExpressionEvaluator.evaluate(
                    "x*y", Map.of("x", 3.0, "y", 4.0));
            assertEquals(12.0, result, DELTA,
                    "x*y with x=3 y=4 should equal 12.0");
        }
    }

    // ------------------------------------------------------------------ //
    //  Nested function calls
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Nested function calls")
    class NestedFunctions {
        @Test
        @DisplayName("nested functions: sqrt(sin(90)) = 1")
        void nestedFunctions() {
            assertEquals(1.0,
                    ExpressionEvaluator.evaluate("sqrt(sin(90))"), DELTA,
                    "sqrt(sin(90)) should equal 1.0");
        }
    }

    // ------------------------------------------------------------------ //
    //  Error cases
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {
        @Test
        @DisplayName("null expression throws IllegalArgumentException")
        void nullExpression() {
            assertThrows(IllegalArgumentException.class,
                    () -> ExpressionEvaluator.evaluate(null),
                    "Null expression should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("empty expression throws IllegalArgumentException")
        void emptyExpression() {
            assertThrows(IllegalArgumentException.class,
                    () -> ExpressionEvaluator.evaluate(""),
                    "Empty expression should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("incomplete expression '2+' throws")
        void incompleteExpr() {
            assertThrows(IllegalArgumentException.class,
                    () -> ExpressionEvaluator.evaluate("2+"),
                    "Incomplete expression should throw");
        }

        @Test
        @DisplayName("unknown variable 'z' throws")
        void unknownVariable() {
            assertThrows(IllegalArgumentException.class,
                    () -> ExpressionEvaluator.evaluate("z"),
                    "Unknown variable should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("unknown function 'foo(1)' throws")
        void unknownFunction() {
            assertThrows(IllegalArgumentException.class,
                    () -> ExpressionEvaluator.evaluate("foo(1)"),
                    "Unknown function should throw IllegalArgumentException");
        }
    }
}
