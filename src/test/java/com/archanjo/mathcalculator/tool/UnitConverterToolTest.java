package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnitConverterToolTest {

    private static final String ERROR_PREFIX = "Error:";

    private final UnitConverterTool tool = new UnitConverterTool();

    @Nested
    @DisplayName("convert")
    class Convert {

        @Test
        void celsiusToFahrenheit() {
            assertEquals("212",
                    tool.convert("100", "c", "f", "TEMPERATURE"),
                    "100 C = 212 F");
        }

        @Test
        void kilometerToMileNotError() {
            assertEquals(false,
                    tool.convert("1", "km", "mi", "LENGTH")
                            .startsWith(ERROR_PREFIX),
                    "Should not be an error");
        }

        @Test
        void wrongCategoryReturnsError() {
            assertTrue(
                    tool.convert("1", "km", "mi", "MASS")
                            .startsWith(ERROR_PREFIX),
                    "Wrong category should return error");
        }

        @Test
        void invalidCategoryReturnsError() {
            assertTrue(
                    tool.convert("1", "km", "mi", "INVALID")
                            .startsWith(ERROR_PREFIX),
                    "Invalid category should return error");
        }

        @Test
        void unknownUnitReturnsError() {
            assertTrue(
                    tool.convert("1", "xxx", "km", "LENGTH")
                            .startsWith(ERROR_PREFIX),
                    "Unknown unit should return error");
        }
    }

    @Nested
    @DisplayName("convertAutoDetect")
    class AutoDetect {

        @Test
        void autoDetectLength() {
            assertEquals("1000",
                    tool.convertAutoDetect("1", "km", "m"),
                    "1 km = 1000 m");
        }

        @Test
        void autoDetectTemperature() {
            assertEquals("32",
                    tool.convertAutoDetect("0", "c", "f"),
                    "0 C = 32 F");
        }

        @Test
        void autoDetectCrossCategory() {
            assertTrue(
                    tool.convertAutoDetect("1", "km", "kg")
                            .startsWith(ERROR_PREFIX),
                    "Cross-category should return error");
        }

        @Test
        void autoDetectRoundTrip() {
            final String toMi =
                    tool.convertAutoDetect("1", "km", "mi");
            final String back =
                    tool.convertAutoDetect(toMi, "mi", "km");
            assertTrue(
                    new BigDecimal(back).subtract(BigDecimal.ONE,
                                    MathContext.DECIMAL128).abs()
                            .compareTo(
                                    new BigDecimal("0.0000000001")) < 0,
                    "Round-trip should return ~1");
        }
    }
}
