package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"preview", "incubating"})
class VectorCalculatorToolTest {

    private static final double DELTA = 1e-10;
    private static final String VECTOR_123 = "1,2,3";
    private static final String VECTOR_1_2_3 = "1.0,2.0,3.0";
    private static final String SCALE_TWO = "2.0";

    private final VectorCalculatorTool tool = new VectorCalculatorTool();

    @Nested
    @DisplayName("sumArray")
    class SumArrayTest {

        @Test
        void sumArrayMultipleElements() {
            final double result = Double.parseDouble(tool.sumArray(VECTOR_1_2_3));
            assertEquals(6.0, result, DELTA,
                    "Sum of [1.0, 2.0, 3.0] should be 6.0");
        }

        @Test
        void sumArraySingleElement() {
            final double result = Double.parseDouble(tool.sumArray("5.0"));
            assertEquals(5.0, result, DELTA,
                    "Sum of single element [5.0] should be 5.0");
        }

        @Test
        void sumArrayEmptyThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.sumArray(""),
                    "Empty input should throw NumberFormatException");
        }
    }

    @Nested
    @DisplayName("dotProduct")
    class DotProductTest {

        @Test
        void dotProductSimple() {
            final double result = Double.parseDouble(
                    tool.dotProduct(VECTOR_123, "4,5,6"));
            assertEquals(32.0, result, DELTA,
                    "Dot product of [1,2,3] and [4,5,6] should be 32.0");
        }

        @Test
        void dotProductMismatchedLengthsThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.dotProduct(VECTOR_123, "4,5"),
                    "Mismatched vector lengths should throw IllegalArgumentException");
        }

        @Test
        void dotProductEmptyFirstThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.dotProduct("", VECTOR_123),
                    "Empty first vector should throw NumberFormatException");
        }

        @Test
        void dotProductEmptySecondThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.dotProduct(VECTOR_123, ""),
                    "Empty second vector should throw NumberFormatException");
        }
    }

    @Nested
    @DisplayName("scaleArray")
    class ScaleArrayTest {

        @Test
        void scaleArrayByTwoReturnsCorrectLength() {
            final String result = tool.scaleArray(VECTOR_1_2_3, SCALE_TWO);
            final String[] parts = result.split(",");
            assertEquals(3, parts.length,
                    "Scaled array should contain 3 elements");
        }

        @Test
        void scaleArrayByTwoFirstElement() {
            final String result = tool.scaleArray(VECTOR_1_2_3, SCALE_TWO);
            final double first = Double.parseDouble(result.split(",")[0]);
            assertEquals(2.0, first, DELTA,
                    "First element 1.0 scaled by 2.0 should be 2.0");
        }

        @Test
        void scaleArrayByTwoSecondElement() {
            final String result = tool.scaleArray(VECTOR_1_2_3, SCALE_TWO);
            final double second = Double.parseDouble(result.split(",")[1]);
            assertEquals(4.0, second, DELTA,
                    "Second element 2.0 scaled by 2.0 should be 4.0");
        }

        @Test
        void scaleArrayByTwoThirdElement() {
            final String result = tool.scaleArray(VECTOR_1_2_3, SCALE_TWO);
            final double third = Double.parseDouble(result.split(",")[2]);
            assertEquals(6.0, third, DELTA,
                    "Third element 3.0 scaled by 2.0 should be 6.0");
        }

        @Test
        void scaleArrayEmptyThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.scaleArray("", SCALE_TWO),
                    "Empty input should throw NumberFormatException");
        }
    }

    @Nested
    @DisplayName("magnitudeArray")
    class MagnitudeArrayTest {

        @Test
        void magnitudeArrayPythagoreanTriple() {
            final double result = Double.parseDouble(
                    tool.magnitudeArray("3.0,4.0"));
            assertEquals(5.0, result, DELTA,
                    "Magnitude of [3.0, 4.0] should be 5.0");
        }

        @Test
        void magnitudeArraySingleElement() {
            final double result = Double.parseDouble(
                    tool.magnitudeArray("5.0"));
            assertEquals(5.0, result, DELTA,
                    "Magnitude of single element [5.0] should be 5.0");
        }

        @Test
        void magnitudeArrayEmptyThrows() {
            assertThrows(NumberFormatException.class,
                    () -> tool.magnitudeArray(""),
                    "Empty input should throw NumberFormatException");
        }
    }
}
