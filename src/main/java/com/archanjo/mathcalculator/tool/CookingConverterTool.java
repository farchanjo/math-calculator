package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.UnitRegistry;

@Component
public class CookingConverterTool {

    private static final Set<String> VOLUME_UNITS =
            Set.of("l", "ml", "uscup", "cup", "tbsp", "tsp",
                    "usfloz", "floz", "usgal", "gal", "igal");
    private static final Map<String, String> VOLUME_ALIASES =
            Map.of("cup", "uscup", "floz", "usfloz", "gal", "usgal");
    private static final Set<String> WEIGHT_UNITS =
            Set.of("kg", "g", "mg", "lb", "oz");
    private static final Set<String> TEMP_UNITS =
            Set.of("c", "f");
    private static final String COOKING_VOLUME = "cooking volume";
    private static final String COOKING_WEIGHT = "cooking weight";
    private static final String GAS_MARK = "gasmark";

    @Tool(description = """
            Convert cooking volume units.""")
    public String convertCookingVolume(
            @ToolParam(description = """
                    Numeric value.""") final String value,
            @ToolParam(description = """
                    Source volume unit.""") final String fromUnit,
            @ToolParam(description = """
                    Target volume unit.""") final String toUnit) {

        String result;
        try {
            validateAllowed(fromUnit, VOLUME_UNITS, COOKING_VOLUME);
            validateAllowed(toUnit, VOLUME_UNITS, COOKING_VOLUME);
            final String from = resolveAlias(fromUnit, VOLUME_ALIASES);
            final String dest = resolveAlias(toUnit, VOLUME_ALIASES);
            result = strip(UnitRegistry.convert(
                    new BigDecimal(value), from, dest));
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Convert cooking weight units.""")
    public String convertCookingWeight(
            @ToolParam(description = """
                    Numeric value.""") final String value,
            @ToolParam(description = """
                    Source weight unit.""") final String fromUnit,
            @ToolParam(description = """
                    Target weight unit.""") final String toUnit) {

        String result;
        try {
            validateAllowed(fromUnit, WEIGHT_UNITS, COOKING_WEIGHT);
            validateAllowed(toUnit, WEIGHT_UNITS, COOKING_WEIGHT);
            result = strip(UnitRegistry.convert(
                    new BigDecimal(value), fromUnit, toUnit));
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Convert oven temperature units.""")
    public String convertOvenTemperature(
            @ToolParam(description = """
                    Temperature value.""") final String value,
            @ToolParam(description = """
                    Source: c, f, or gasmark.""") final String fromUnit,
            @ToolParam(description = """
                    Target: c, f, or gasmark.""") final String toUnit) {

        String result;
        try {
            final String source = fromUnit.toLowerCase(Locale.ROOT);
            final String target = toUnit.toLowerCase(Locale.ROOT);
            final BigDecimal input = new BigDecimal(value);
            final BigDecimal celsius = ovenToCelsius(input, source);
            result = strip(celsiusToOven(celsius, target));
        } catch (IllegalArgumentException ex) {
            result = "Error: " + ex.getMessage();
        }
        return result;
    }

    private BigDecimal ovenToCelsius(
            final BigDecimal input, final String source) {

        final BigDecimal result;
        if (GAS_MARK.equals(source)) {
            result = UnitRegistry.gasMarkToCelsius(
                    input.intValueExact());
        } else {
            validateOvenUnit(source);
            result = UnitRegistry.toCelsius(source, input);
        }
        return result;
    }

    private BigDecimal celsiusToOven(
            final BigDecimal celsius, final String target) {

        final BigDecimal result;
        if (GAS_MARK.equals(target)) {
            result = BigDecimal.valueOf(
                    UnitRegistry.celsiusToGasMark(celsius));
        } else {
            validateOvenUnit(target);
            result = UnitRegistry.fromCelsius(target, celsius);
        }
        return result;
    }

    private void validateOvenUnit(final String code) {
        if (!TEMP_UNITS.contains(code)) {
            throw new IllegalArgumentException(
                    "Oven temperature unit must be c, f, or gasmark."
                            + " Received: " + code);
        }
    }

    private String resolveAlias(final String code,
                               final Map<String, String> aliases) {
        final String lower = code.toLowerCase(Locale.ROOT);
        return aliases.getOrDefault(lower, lower);
    }

    private void validateAllowed(
            final String code, final Set<String> allowed,
            final String kind) {

        if (!allowed.contains(code.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "'%s' is not a valid %s unit".formatted(code, kind));
        }
    }

    private String strip(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
