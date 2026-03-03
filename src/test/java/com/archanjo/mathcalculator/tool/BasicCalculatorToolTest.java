package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BasicCalculatorToolTest {

    private static final String TWO_POINT_FIVE = "2.5";
    private static final String NOT_THROWN = "<<NO EXCEPTION WAS THROWN>>";

    private final BasicCalculatorTool tool = new BasicCalculatorTool();

    @Nested
    @DisplayName("add")
    class AddTests {

        @Test
        void addSimpleDecimals() {
            assertEquals("4.0", tool.add("1.5", TWO_POINT_FIVE),
                    "Adding 1.5 and 2.5 should produce 4.0");
        }

        @Test
        void addLargeNumbers() {
            assertEquals(
                    "99999999999999999999999999999999999999999",
                    tool.add(
                            "99999999999999999999999999999999999999990",
                            "9"),
                    "Adding large numbers should produce correct sum");
        }

        @Test
        void addNegativeNumbers() {
            assertEquals("-5.5", tool.add("-3.0", "-2.5"),
                    "Adding two negative numbers should produce negative sum");
        }

        @Test
        void addPositiveAndNegative() {
            assertEquals("0", tool.add("5", "-5"),
                    "Adding a number and its negation should produce zero");
        }

        @Test
        void addZeros() {
            assertEquals("0", tool.add("0", "0"),
                    "Adding two zeros should produce zero");
        }
    }

    @Nested
    @DisplayName("subtract")
    class SubtractTests {

        @Test
        void subtractSimple() {
            assertEquals("7", tool.subtract("10", "3"),
                    "Subtracting 3 from 10 should produce 7");
        }

        @Test
        void subtractNegativeResult() {
            assertEquals("-5", tool.subtract("3", "8"),
                    "Subtracting larger from smaller should produce negative result");
        }

        @Test
        void subtractSameNumber() {
            assertEquals("0", tool.subtract("42", "42"),
                    "Subtracting a number from itself should produce zero");
        }

        @Test
        void subtractDecimals() {
            assertEquals("0.3", tool.subtract("1.5", "1.2"),
                    "Subtracting decimals should produce correct difference");
        }
    }

    @Nested
    @DisplayName("multiply")
    class MultiplyTests {

        @Test
        void multiplySimple() {
            assertEquals("12", tool.multiply("3", "4"),
                    "Multiplying 3 by 4 should produce 12");
        }

        @Test
        void multiplyDecimals() {
            assertEquals("6.25", tool.multiply(TWO_POINT_FIVE, TWO_POINT_FIVE),
                    "Multiplying 2.5 by 2.5 should produce 6.25");
        }

        @Test
        void multiplyByZero() {
            assertEquals("0", tool.multiply("999999", "0"),
                    "Multiplying any number by zero should produce zero");
        }

        @Test
        void multiplyNegatives() {
            assertEquals("15", tool.multiply("-3", "-5"),
                    "Multiplying two negatives should produce positive result");
        }

        @Test
        void multiplyMixedSign() {
            assertEquals("-20", tool.multiply("4", "-5"),
                    "Multiplying positive by negative should produce negative result");
        }
    }

    @Nested
    @DisplayName("divide")
    class DivideTests {

        @Test
        void divideExact() {
            assertEquals("5", tool.divide("10", "2"),
                    "Dividing 10 by 2 should produce 5");
        }

        @Test
        void divideRepeatingDecimal() {
            final String result = tool.divide("10", "3");
            assertTrue(result.startsWith("3.33333"),
                    "Expected result to start with 3.33333 but was: " + result);
        }

        @Test
        void divideByZeroThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.divide("10", "0"),
                    "Dividing by zero should throw IllegalArgumentException");
        }

        @Test
        void divideByZeroExceptionMessage() {
            final String message = extractExceptionMessage(() -> tool.divide("10", "0"));
            assertEquals("Division by zero", message,
                    "Exception message should indicate division by zero");
        }

        @Test
        void divideNegative() {
            assertEquals("-2.5", tool.divide("5", "-2"),
                    "Dividing positive by negative should produce negative result");
        }

        @Test
        void divideSmallByLarge() {
            final String result = tool.divide("1", "7");
            assertTrue(result.startsWith("0.14285714285"),
                    "Expected repeating fraction but was: " + result);
        }

        @Test
        void divideStripsTrailingZeros() {
            assertEquals(TWO_POINT_FIVE, tool.divide("5", "2"),
                    "Dividing 5 by 2 should produce 2.5 without trailing zeros");
        }
    }

    @Nested
    @DisplayName("power")
    class PowerTests {

        @Test
        void powerSimple() {
            assertEquals("1024", tool.power("2", "10"),
                    "2 raised to the power of 10 should produce 1024");
        }

        @Test
        void powerZeroExponent() {
            assertEquals("1", tool.power("999", "0"),
                    "Any number raised to power zero should produce 1");
        }

        @Test
        void powerOne() {
            assertEquals("5", tool.power("5", "1"),
                    "Any number raised to power one should return itself");
        }

        @Test
        void powerNegativeExponentThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.power("2", "-1"),
                    "Negative exponent should throw IllegalArgumentException");
        }

        @Test
        void powerNegativeExponentExceptionMessage() {
            final String message = extractExceptionMessage(() -> tool.power("2", "-1"));
            assertEquals("Exponent must be a non-negative integer", message,
                    "Exception message should indicate non-negative integer requirement");
        }

        @Test
        void powerDecimalBase() {
            assertEquals("6.25", tool.power(TWO_POINT_FIVE, "2"),
                    "2.5 raised to power 2 should produce 6.25");
        }

        @Test
        void powerZeroBase() {
            assertEquals("0", tool.power("0", "5"),
                    "Zero raised to any positive power should produce zero");
        }
    }

    @Nested
    @DisplayName("modulo")
    class ModuloTests {

        @Test
        void moduloSimple() {
            assertEquals("1", tool.modulo("10", "3"),
                    "10 modulo 3 should produce 1");
        }

        @Test
        void moduloByZeroThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.modulo("10", "0"),
                    "Modulo by zero should throw IllegalArgumentException");
        }

        @Test
        void moduloByZeroExceptionMessage() {
            final String message = extractExceptionMessage(() -> tool.modulo("10", "0"));
            assertEquals("Division by zero", message,
                    "Exception message should indicate division by zero");
        }

        @Test
        void moduloExactDivision() {
            assertEquals("0", tool.modulo("9", "3"),
                    "Modulo of evenly divisible numbers should produce zero");
        }

        @Test
        void moduloDecimal() {
            assertEquals("0.5", tool.modulo("10.5", "2"),
                    "Modulo with decimal dividend should produce correct remainder");
        }

        @Test
        void moduloNegativeDividend() {
            assertEquals("-1", tool.modulo("-10", "3"),
                    "Modulo with negative dividend should preserve sign");
        }
    }

    @Nested
    @DisplayName("abs")
    class AbsTests {

        @Test
        void absNegative() {
            assertEquals("5", tool.abs("-5"),
                    "Absolute value of negative number should be positive");
        }

        @Test
        void absPositive() {
            assertEquals("5", tool.abs("5"),
                    "Absolute value of positive number should remain the same");
        }

        @Test
        void absZero() {
            assertEquals("0", tool.abs("0"),
                    "Absolute value of zero should be zero");
        }

        @Test
        void absNegativeDecimal() {
            assertEquals("3.14", tool.abs("-3.14"),
                    "Absolute value of negative decimal should be positive");
        }
    }

    @Nested
    @DisplayName("invalid input")
    class InvalidInputTests {

        @Test
        void addInvalidInputThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.add("abc", "1"),
                    "Non-numeric input should throw NumberFormatException");
        }

        @Test
        void divideInvalidInputThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.divide("abc", "1"),
                    "Non-numeric input should throw NumberFormatException");
        }

        @Test
        void powerNonIntegerExponentThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.power("2", "1.5"),
                    "Decimal exponent should throw NumberFormatException");
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static String extractExceptionMessage(final Runnable action) {
        String result = NOT_THROWN;
        try {
            action.run();
        } catch (final Exception exception) {
            result = exception.getMessage();
        }
        return result;
    }
}
