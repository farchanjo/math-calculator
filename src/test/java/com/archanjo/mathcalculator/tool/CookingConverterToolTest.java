package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CookingConverterToolTest {

    private static final String ERROR_PREFIX = "Error:";
    private static final BigDecimal CLOSE_ENOUGH = new BigDecimal("0.01");

    private final CookingConverterTool tool = new CookingConverterTool();

    @Nested
    @DisplayName("convertCookingVolume")
    class CookingVolume {

        @Test
        void cupToTablespoon() {
            final String result = tool.convertCookingVolume(
                    "1", "uscup", "tbsp");
            final BigDecimal value = new BigDecimal(result);
            assertTrue(value.subtract(new BigDecimal("16")).abs()
                            .compareTo(CLOSE_ENOUGH) < 0,
                    "1 US cup = 16 tbsp");
        }

        @Test
        void tablespoonToTeaspoon() {
            final String result = tool.convertCookingVolume(
                    "1", "tbsp", "tsp");
            final BigDecimal value = new BigDecimal(result);
            assertTrue(value.subtract(new BigDecimal("3")).abs()
                            .compareTo(CLOSE_ENOUGH) < 0,
                    "1 tbsp = 3 tsp");
        }

        @Test
        void cupAliasToTablespoon() {
            final String result = tool.convertCookingVolume(
                    "1", "cup", "tbsp");
            final BigDecimal value = new BigDecimal(result);
            assertTrue(value.subtract(new BigDecimal("16")).abs()
                            .compareTo(CLOSE_ENOUGH) < 0,
                    "1 cup (alias) = 16 tbsp");
        }

        @Test
        void flozAliasToMl() {
            final String result = tool.convertCookingVolume(
                    "1", "floz", "ml");
            final BigDecimal value = new BigDecimal(result);
            assertTrue(value.subtract(new BigDecimal("29.57")).abs()
                            .compareTo(new BigDecimal("0.1")) < 0,
                    "1 floz (alias) ≈ 29.57 ml");
        }

        @Test
        void galAliasToLiter() {
            final String result = tool.convertCookingVolume(
                    "1", "gal", "l");
            final BigDecimal value = new BigDecimal(result);
            assertTrue(value.subtract(new BigDecimal("3.785")).abs()
                            .compareTo(CLOSE_ENOUGH) < 0,
                    "1 gal (alias) ≈ 3.785 l");
        }

        @Test
        void invalidVolumeUnitReturnsError() {
            final String result = tool.convertCookingVolume(
                    "1", "kg", "ml");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "kg is not a volume unit");
        }
    }

    @Nested
    @DisplayName("convertCookingWeight")
    class CookingWeight {

        @Test
        void poundToOunce() {
            final String result = tool.convertCookingWeight("1", "lb", "oz");
            final BigDecimal value = new BigDecimal(result);
            assertTrue(value.subtract(new BigDecimal("16")).abs()
                            .compareTo(CLOSE_ENOUGH) < 0,
                    "1 lb ≈ 16 oz");
        }

        @Test
        void gramToKilogram() {
            final String result = tool.convertCookingWeight(
                    "1000", "g", "kg");
            assertEquals("1", result, "1000 g = 1 kg");
        }

        @Test
        void invalidWeightUnitReturnsError() {
            final String result = tool.convertCookingWeight(
                    "1", "ml", "g");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "ml is not a weight unit");
        }
    }

    @Nested
    @DisplayName("convertOvenTemperature")
    class OvenTemperature {

        @Test
        void gasMarkToCelsius() {
            final String result = tool.convertOvenTemperature(
                    "4", "gasmark", "c");
            assertEquals("180", result, "Gas mark 4 = 180 C");
        }

        @Test
        void celsiusToGasMark() {
            final String result = tool.convertOvenTemperature(
                    "200", "c", "gasmark");
            assertEquals("6", result, "200 C ≈ gas mark 6");
        }

        @Test
        void celsiusToFahrenheit() {
            final String result = tool.convertOvenTemperature(
                    "180", "c", "f");
            assertEquals("356", result, "180 C = 356 F");
        }

        @Test
        void gasMarkToFahrenheit() {
            final String result = tool.convertOvenTemperature(
                    "4", "gasmark", "f");
            assertEquals("356", result, "Gas mark 4 = 356 F");
        }

        @Test
        void invalidOvenUnitReturnsError() {
            final String result = tool.convertOvenTemperature(
                    "100", "k", "c");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Kelvin is not a valid oven unit");
        }
    }
}
