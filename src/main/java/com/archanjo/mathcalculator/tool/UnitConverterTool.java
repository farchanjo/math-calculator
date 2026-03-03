package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.UnitCategory;
import com.archanjo.mathcalculator.engine.UnitDefinition;
import com.archanjo.mathcalculator.engine.UnitRegistry;

@Component
public class UnitConverterTool {

    @Tool(description = """
            Convert a value between measurement units.""")
    public String convert(
            @ToolParam(description = """
                    Numeric value.""") final String value,
            @ToolParam(description = """
                    Source unit code.""") final String fromUnit,
            @ToolParam(description = """
                    Target unit code.""") final String toUnit,
            @ToolParam(description = """
                    Category name.""") final String category) {

        String result;
        try {
            final UnitCategory cat = UnitCategory.valueOf(
                    category.toUpperCase(Locale.ROOT));
            validateCategory(fromUnit, cat);
            validateCategory(toUnit, cat);
            result = strip(UnitRegistry.convert(
                    new BigDecimal(value), fromUnit, toUnit));
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Convert units with auto-detected category.""")
    public String convertAutoDetect(
            @ToolParam(description = """
                    Numeric value.""") final String value,
            @ToolParam(description = """
                    Source unit code.""") final String fromUnit,
            @ToolParam(description = """
                    Target unit code.""") final String toUnit) {

        String result;
        try {
            result = strip(UnitRegistry.convert(
                    new BigDecimal(value), fromUnit, toUnit));
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    private void validateCategory(
            final String code, final UnitCategory expected) {

        final UnitDefinition def = UnitRegistry.findUnit(code);
        if (def == null) {
            throw new IllegalArgumentException(
                    "Unknown unit: " + code);
        }
        if (def.category() != expected) {
            throw new IllegalArgumentException(
                    "Unit '%s' is not in category %s".formatted(
                            code, expected));
        }
    }

    private String strip(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
