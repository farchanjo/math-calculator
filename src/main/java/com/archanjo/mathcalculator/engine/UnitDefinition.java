package com.archanjo.mathcalculator.engine;

import java.math.BigDecimal;

/**
 * Defines a unit of measurement with its conversion factor to the base unit.
 *
 * @param code         short lowercase code (e.g. "km", "lb", "psi")
 * @param name         human-readable name (e.g. "kilometer", "pound")
 * @param category     the measurement category this unit belongs to
 * @param toBaseFactor multiplier to convert a value in this unit to the base unit;
 *                     {@code null} for units that require formula-based conversion (temperature)
 */
public record UnitDefinition(
        String code,
        String name,
        UnitCategory category,
        BigDecimal toBaseFactor) {
}
