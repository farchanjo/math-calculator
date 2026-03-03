package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MeasureReferenceToolTest {

    private static final String ERROR_PREFIX = "Error:";

    private final MeasureReferenceTool tool = new MeasureReferenceTool();

    @Nested
    @DisplayName("listCategories")
    class ListCategories {

        @Test
        void returnsJsonArrayStart() {
            assertTrue(tool.listCategories().startsWith("["),
                    "Should start with [");
        }

        @Test
        void returnsJsonArrayEnd() {
            assertTrue(tool.listCategories().endsWith("]"),
                    "Should end with ]");
        }

        @Test
        void containsLength() {
            assertTrue(tool.listCategories().contains("LENGTH"),
                    "Should have LENGTH");
        }

        @Test
        void containsTemperature() {
            assertTrue(
                    tool.listCategories().contains("TEMPERATURE"),
                    "Should have TEMPERATURE");
        }
    }

    @Nested
    @DisplayName("listUnits")
    class ListUnits {

        @Test
        void lengthUnitsContainKm() {
            assertTrue(tool.listUnits("LENGTH").contains("\"km\""),
                    "LENGTH should include km");
        }

        @Test
        void invalidCategoryReturnsError() {
            assertTrue(
                    tool.listUnits("INVALID").startsWith(ERROR_PREFIX),
                    "Invalid category should return error");
        }
    }

    @Nested
    @DisplayName("getConversionFactor")
    class ConversionFactor {

        @Test
        void kmToM() {
            assertTrue(
                    tool.getConversionFactor("km", "m")
                            .contains("1000"),
                    "1 km = 1000 m factor");
        }

        @Test
        void temperatureReturnsError() {
            assertTrue(
                    tool.getConversionFactor("c", "f")
                            .startsWith(ERROR_PREFIX),
                    "Temperature uses formulas, not factors");
        }

        @Test
        void crossCategoryReturnsError() {
            assertTrue(
                    tool.getConversionFactor("km", "kg")
                            .startsWith(ERROR_PREFIX),
                    "Cross-category should return error");
        }
    }

    @Nested
    @DisplayName("explainConversion")
    class ExplainConversion {

        @Test
        void linearConversionMentionsSourceUnit() {
            assertTrue(
                    tool.explainConversion("km", "mi")
                            .contains("kilometer"),
                    "Should mention source unit name");
        }

        @Test
        void linearConversionMentionsTargetUnit() {
            assertTrue(
                    tool.explainConversion("km", "mi")
                            .contains("mile"),
                    "Should mention target unit name");
        }

        @Test
        void temperatureExplanation() {
            final String result = tool.explainConversion("c", "f");
            assertEquals(true,
                    result.contains("9/5") || result.contains("32"),
                    "Should contain temperature formula");
        }

        @Test
        void unknownUnitReturnsError() {
            assertTrue(
                    tool.explainConversion("xxx", "km")
                            .startsWith(ERROR_PREFIX),
                    "Unknown unit should return error");
        }
    }
}
