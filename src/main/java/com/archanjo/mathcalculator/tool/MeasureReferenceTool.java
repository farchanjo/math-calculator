package com.archanjo.mathcalculator.tool;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.UnitCategory;
import com.archanjo.mathcalculator.engine.UnitDefinition;
import com.archanjo.mathcalculator.engine.UnitRegistry;

@Component
public class MeasureReferenceTool {

    @Tool(description = """
            List all unit conversion categories.""")
    public String listCategories() {
        final List<UnitCategory> categories =
                UnitRegistry.listCategories();
        return categories.stream()
                .map(cat -> "{\"name\":\"%s\"}".formatted(cat.name()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Tool(description = """
            List units in a category.""")
    public String listUnits(
            @ToolParam(description = """
                    Category name.""") final String category) {

        String result;
        try {
            final UnitCategory cat = UnitCategory.valueOf(
                    category.toUpperCase(Locale.ROOT));
            final List<UnitDefinition> units =
                    UnitRegistry.listUnits(cat);
            result = units.stream()
                    .map(unit -> "{\"code\":\"%s\",\"name\":\"%s\"}"
                            .formatted(unit.code(), unit.name()))
                    .collect(Collectors.joining(",", "[", "]"));
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Get conversion factor between two units.""")
    public String getConversionFactor(
            @ToolParam(description = """
                    Source unit code.""") final String fromUnit,
            @ToolParam(description = """
                    Target unit code.""") final String toUnit) {

        String result;
        try {
            final java.math.BigDecimal factor =
                    UnitRegistry.getConversionFactor(fromUnit, toUnit);
            result = factor.stripTrailingZeros().toPlainString();
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Explain conversion formula between units.""")
    public String explainConversion(
            @ToolParam(description = """
                    Source unit code.""") final String fromUnit,
            @ToolParam(description = """
                    Target unit code.""") final String toUnit) {

        String result;
        try {
            result = UnitRegistry.explainConversion(fromUnit, toUnit);
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }
}
