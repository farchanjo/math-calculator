package com.archanjo.mathcalculator.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Static registry of unit definitions and conversion logic for all 21 categories.
 *
 * <p>Linear conversions use {@code value * from.toBaseFactor / to.toBaseFactor}.
 * Temperature uses formula-based conversion through Celsius.
 * Gas mark uses a lookup table.
 */
public final class UnitRegistry {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int FACTOR_SCALE = 34;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // Reusable literal constants (PMD: AvoidDuplicateLiterals)
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal SECONDS_PER_HOUR = new BigDecimal("3600");
    private static final BigDecimal MILLI = new BigDecimal("0.001");
    private static final BigDecimal SIXTY = new BigDecimal("60");

    // Exact SI building blocks
    private static final BigDecimal POUND_KG = new BigDecimal("0.45359237");
    private static final BigDecimal GRAVITY = new BigDecimal("9.80665");
    private static final BigDecimal INCH_M = new BigDecimal("0.0254");
    private static final BigDecimal FOOT_M = new BigDecimal("0.3048");
    private static final BigDecimal LBF_N =
            POUND_KG.multiply(GRAVITY, PRECISION);
    private static final BigDecimal PI_VALUE = new BigDecimal(
            "3.1415926535897932384626433832795028841972");

    // Derived factors (computed once at class load)
    private static final BigDecimal PSI_PA =
            LBF_N.divide(INCH_M.pow(2, PRECISION), FACTOR_SCALE, ROUNDING);
    private static final BigDecimal TORR_PA = new BigDecimal("101325")
            .divide(new BigDecimal("760"), FACTOR_SCALE, ROUNDING);
    private static final BigDecimal HP_W = new BigDecimal("550")
            .multiply(FOOT_M, PRECISION).multiply(LBF_N, PRECISION);
    private static final BigDecimal KMH_MS =
            THOUSAND.divide(SECONDS_PER_HOUR, FACTOR_SCALE, ROUNDING);
    private static final BigDecimal KNOT_MS = new BigDecimal("1852")
            .divide(SECONDS_PER_HOUR, FACTOR_SCALE, ROUNDING);
    private static final BigDecimal DEG_PER_RAD = new BigDecimal("180")
            .divide(PI_VALUE, FACTOR_SCALE, ROUNDING);
    private static final BigDecimal RPM_HZ =
            BigDecimal.ONE.divide(SIXTY, FACTOR_SCALE, ROUNDING);
    private static final BigDecimal BTU_H_W = new BigDecimal("1055.05585262")
            .divide(SECONDS_PER_HOUR, FACTOR_SCALE, ROUNDING);
    private static final BigDecimal ARCMIN_DEG =
            BigDecimal.ONE.divide(SIXTY, FACTOR_SCALE, ROUNDING);
    private static final BigDecimal ARCSEC_DEG =
            BigDecimal.ONE.divide(SECONDS_PER_HOUR, FACTOR_SCALE, ROUNDING);

    // Electrical/Data rate prefix constants
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal BILLION = new BigDecimal("1000000000");
    private static final BigDecimal TRILLION = new BigDecimal("1000000000000");
    private static final BigDecimal EIGHT = new BigDecimal("8");
    private static final BigDecimal MICRO = new BigDecimal("0.000001");
    private static final BigDecimal NANO = new BigDecimal("0.000000001");
    private static final BigDecimal PICO = new BigDecimal("0.000000000001");

    // Temperature constants
    private static final BigDecimal NINE = new BigDecimal("9");
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal THIRTY_TWO = new BigDecimal("32");
    private static final BigDecimal KELVIN_OFFSET = new BigDecimal("273.15");
    private static final BigDecimal RANKINE_OFFSET = new BigDecimal("491.67");
    private static final BigDecimal RANKINE_RATIO =
            FIVE.divide(NINE, FACTOR_SCALE, ROUNDING);

    private static final Map<String, UnitDefinition> UNITS =
            new LinkedHashMap<>(128);
    private static final Map<UnitCategory, String> BASE_UNITS =
            new EnumMap<>(UnitCategory.class);
    private static final NavigableMap<Integer, BigDecimal> GAS_MARK_TO_C =
            new TreeMap<>();

    private static final Map<String, String> TEMP_FORMULAS = Map.ofEntries(
            Map.entry("c->f", "F = C * 9/5 + 32"),
            Map.entry("f->c", "C = (F - 32) * 5/9"),
            Map.entry("c->k", "K = C + 273.15"),
            Map.entry("k->c", "C = K - 273.15"),
            Map.entry("c->r", "R = C * 9/5 + 491.67"),
            Map.entry("r->c", "C = (R - 491.67) * 5/9"),
            Map.entry("f->k", "K = (F - 32) * 5/9 + 273.15"),
            Map.entry("k->f", "F = (K - 273.15) * 9/5 + 32"),
            Map.entry("f->r", "R = F + 459.67"),
            Map.entry("r->f", "F = R - 459.67"),
            Map.entry("k->r", "R = K * 9/5"),
            Map.entry("r->k", "K = R * 5/9"));

    static {
        registerDataStorage();
        registerLength();
        registerMass();
        registerVolume();
        registerTemperature();
        registerTime();
        registerSpeed();
        registerArea();
        registerEnergy();
        registerForce();
        registerPressure();
        registerPower();
        registerDensity();
        registerFrequency();
        registerAngle();
        registerDataRate();
        registerResistance();
        registerCapacitance();
        registerInductance();
        registerVoltage();
        registerCurrent();
        registerGasMark();
    }

    private UnitRegistry() {
        // static utility — no instantiation
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    public static BigDecimal convert(
            final BigDecimal value,
            final String fromCode,
            final String toCode) {

        final UnitDefinition source = requireUnit(fromCode);
        final UnitDefinition target = requireUnit(toCode);
        requireSameCategory(source, target);

        final BigDecimal result;
        if (source.category() == UnitCategory.TEMPERATURE) {
            result = convertTemperature(
                    value, source.code(), target.code());
        } else {
            result = value.multiply(source.toBaseFactor(), PRECISION)
                    .divide(target.toBaseFactor(), FACTOR_SCALE, ROUNDING);
        }
        return result;
    }

    public static List<UnitCategory> listCategories() {
        return List.of(UnitCategory.values());
    }

    public static List<UnitDefinition> listUnits(
            final UnitCategory category) {

        return UNITS.values().stream()
                .filter(unit -> unit.category() == category)
                .toList();
    }

    public static UnitDefinition findUnit(final String code) {
        return UNITS.get(code.toLowerCase(Locale.ROOT));
    }

    public static BigDecimal getConversionFactor(
            final String fromCode, final String toCode) {

        final UnitDefinition source = requireUnit(fromCode);
        final UnitDefinition target = requireUnit(toCode);
        requireSameCategory(source, target);
        if (source.category() == UnitCategory.TEMPERATURE) {
            throw new IllegalArgumentException(
                    "Temperature uses formulas, not a fixed factor");
        }
        return source.toBaseFactor()
                .divide(target.toBaseFactor(), FACTOR_SCALE, ROUNDING);
    }

    public static String explainConversion(
            final String fromCode, final String toCode) {

        final UnitDefinition source = requireUnit(fromCode);
        final UnitDefinition target = requireUnit(toCode);
        requireSameCategory(source, target);

        final String result;
        if (source.category() == UnitCategory.TEMPERATURE) {
            result = explainTemperature(source.code(), target.code());
        } else {
            final BigDecimal factor =
                    getConversionFactor(fromCode, toCode);
            result = "1 %s = %s %s".formatted(
                    source.name(), strip(factor), target.name());
        }
        return result;
    }

    public static BigDecimal gasMarkToCelsius(final int mark) {
        final BigDecimal celsius = GAS_MARK_TO_C.get(mark);
        if (celsius == null) {
            throw new IllegalArgumentException(
                    "Gas mark must be 1-10. Received: " + mark);
        }
        return celsius;
    }

    public static int celsiusToGasMark(final BigDecimal celsius) {
        int closest = 1;
        BigDecimal minDist = null;
        for (final Map.Entry<Integer, BigDecimal> entry
                : GAS_MARK_TO_C.entrySet()) {
            final BigDecimal dist = celsius
                    .subtract(entry.getValue()).abs();
            if (minDist == null || dist.compareTo(minDist) < 0) {
                minDist = dist;
                closest = entry.getKey();
            }
        }
        return closest;
    }

    public static Map<String, UnitDefinition> allUnits() {
        return Collections.unmodifiableMap(UNITS);
    }

    // ------------------------------------------------------------------ //
    //  Temperature conversion
    // ------------------------------------------------------------------ //

    public static BigDecimal toCelsius(
            final String code, final BigDecimal value) {

        return switch (code) {
            case "c" -> value;
            case "f" -> value.subtract(THIRTY_TWO, PRECISION)
                    .multiply(FIVE, PRECISION)
                    .divide(NINE, FACTOR_SCALE, ROUNDING);
            case "k" -> value.subtract(KELVIN_OFFSET, PRECISION);
            case "r" -> value.subtract(RANKINE_OFFSET, PRECISION)
                    .multiply(RANKINE_RATIO, PRECISION);
            default -> throw new IllegalArgumentException(
                    "Unknown temperature unit: " + code);
        };
    }

    public static BigDecimal fromCelsius(
            final String code, final BigDecimal celsius) {

        return switch (code) {
            case "c" -> celsius;
            case "f" -> celsius.multiply(NINE, PRECISION)
                    .divide(FIVE, FACTOR_SCALE, ROUNDING)
                    .add(THIRTY_TWO, PRECISION);
            case "k" -> celsius.add(KELVIN_OFFSET, PRECISION);
            case "r" -> celsius.divide(RANKINE_RATIO, FACTOR_SCALE, ROUNDING)
                    .add(RANKINE_OFFSET, PRECISION);
            default -> throw new IllegalArgumentException(
                    "Unknown temperature unit: " + code);
        };
    }

    // ------------------------------------------------------------------ //
    //  Registration helpers
    // ------------------------------------------------------------------ //

    private static void reg(
            final String code, final String name,
            final UnitCategory cat, final BigDecimal factor) {

        UNITS.put(code, new UnitDefinition(code, name, cat, factor));
    }

    private static void regBase(
            final String code, final String name,
            final UnitCategory cat) {

        reg(code, name, cat, BigDecimal.ONE);
        BASE_UNITS.put(cat, code);
    }

    private static void regTemp(
            final String code, final String name) {

        UNITS.put(code, new UnitDefinition(
                code, name, UnitCategory.TEMPERATURE, null));
    }

    // ------------------------------------------------------------------ //
    //  Category registration (each < 30 lines)
    // ------------------------------------------------------------------ //

    private static void registerDataStorage() {
        final UnitCategory cat = UnitCategory.DATA_STORAGE;
        regBase("byte", "byte", cat);
        reg("bit", "bit", cat, new BigDecimal("0.125"));
        reg("kb", "kilobyte", cat, new BigDecimal("1024"));
        reg("mb", "megabyte", cat, new BigDecimal("1048576"));
        reg("gb", "gigabyte", cat, new BigDecimal("1073741824"));
        reg("tb", "terabyte", cat, new BigDecimal("1099511627776"));
        reg("pb", "petabyte", cat,
                new BigDecimal("1125899906842624"));
    }

    private static void registerLength() {
        final UnitCategory cat = UnitCategory.LENGTH;
        regBase("m", "meter", cat);
        reg("mm", "millimeter", cat, MILLI);
        reg("cm", "centimeter", cat, new BigDecimal("0.01"));
        reg("km", "kilometer", cat, THOUSAND);
        reg("in", "inch", cat, INCH_M);
        reg("ft", "foot", cat, FOOT_M);
        reg("yd", "yard", cat, new BigDecimal("0.9144"));
        reg("mi", "mile", cat, new BigDecimal("1609.344"));
        reg("nmi", "nautical mile", cat, new BigDecimal("1852"));
    }

    private static void registerMass() {
        final UnitCategory cat = UnitCategory.MASS;
        regBase("kg", "kilogram", cat);
        reg("g", "gram", cat, MILLI);
        reg("mg", "milligram", cat, new BigDecimal("0.000001"));
        reg("t", "tonne", cat, THOUSAND);
        reg("lb", "pound", cat, POUND_KG);
        reg("oz", "ounce", cat, new BigDecimal("0.028349523125"));
        reg("st", "stone", cat, new BigDecimal("6.35029318"));
    }

    private static void registerVolume() {
        final UnitCategory cat = UnitCategory.VOLUME;
        regBase("l", "liter", cat);
        reg("ml", "milliliter", cat, MILLI);
        reg("m3", "cubic meter", cat, THOUSAND);
        reg("usgal", "US gallon", cat,
                new BigDecimal("3.785411784"));
        reg("igal", "imperial gallon", cat,
                new BigDecimal("4.54609"));
        reg("uscup", "US cup", cat,
                new BigDecimal("0.2365882365"));
        reg("tbsp", "tablespoon", cat,
                new BigDecimal("0.01478676478125"));
        reg("tsp", "teaspoon", cat,
                new BigDecimal("0.00492892159375"));
        reg("usfloz", "US fluid ounce", cat,
                new BigDecimal("0.0295735295625"));
    }

    private static void registerTemperature() {
        BASE_UNITS.put(UnitCategory.TEMPERATURE, "c");
        regTemp("c", "Celsius");
        regTemp("f", "Fahrenheit");
        regTemp("k", "Kelvin");
        regTemp("r", "Rankine");
    }

    private static void registerTime() {
        final UnitCategory cat = UnitCategory.TIME;
        regBase("s", "second", cat);
        reg("ms", "millisecond", cat, MILLI);
        reg("min", "minute", cat, SIXTY);
        reg("h", "hour", cat, SECONDS_PER_HOUR);
        reg("d", "day", cat, new BigDecimal("86400"));
        reg("wk", "week", cat, new BigDecimal("604800"));
        reg("yr", "year", cat, new BigDecimal("31557600"));
    }

    private static void registerSpeed() {
        final UnitCategory cat = UnitCategory.SPEED;
        regBase("m/s", "meter per second", cat);
        reg("km/h", "kilometer per hour", cat, KMH_MS);
        reg("mph", "mile per hour", cat,
                new BigDecimal("0.44704"));
        reg("kn", "knot", cat, KNOT_MS);
        reg("ft/s", "foot per second", cat, FOOT_M);
    }

    private static void registerArea() {
        final UnitCategory cat = UnitCategory.AREA;
        regBase("m2", "square meter", cat);
        reg("cm2", "square centimeter", cat,
                new BigDecimal("0.0001"));
        reg("km2", "square kilometer", cat,
                new BigDecimal("1000000"));
        reg("ft2", "square foot", cat,
                new BigDecimal("0.09290304"));
        reg("ac", "acre", cat, new BigDecimal("4046.8564224"));
        reg("ha", "hectare", cat, new BigDecimal("10000"));
        reg("mi2", "square mile", cat,
                new BigDecimal("2589988.110336"));
    }

    private static void registerEnergy() {
        final UnitCategory cat = UnitCategory.ENERGY;
        regBase("j", "joule", cat);
        reg("cal", "calorie", cat, new BigDecimal("4.184"));
        reg("kcal", "kilocalorie", cat, new BigDecimal("4184"));
        reg("kwh", "kilowatt-hour", cat,
                new BigDecimal("3600000"));
        reg("btu", "BTU", cat,
                new BigDecimal("1055.05585262"));
        reg("ev", "electronvolt", cat,
                new BigDecimal("1.602176634E-19"));
    }

    private static void registerForce() {
        final UnitCategory cat = UnitCategory.FORCE;
        regBase("n", "newton", cat);
        reg("dyn", "dyne", cat, new BigDecimal("0.00001"));
        reg("lbf", "pound-force", cat, LBF_N);
        reg("kgf", "kilogram-force", cat, GRAVITY);
    }

    private static void registerPressure() {
        final UnitCategory cat = UnitCategory.PRESSURE;
        regBase("pa", "pascal", cat);
        reg("bar", "bar", cat, new BigDecimal("100000"));
        reg("atm", "atmosphere", cat, new BigDecimal("101325"));
        reg("psi", "pound per square inch", cat, PSI_PA);
        reg("torr", "torr", cat, TORR_PA);
        reg("mmhg", "millimeter of mercury", cat,
                new BigDecimal("133.322387415"));
    }

    private static void registerPower() {
        final UnitCategory cat = UnitCategory.POWER;
        regBase("w", "watt", cat);
        reg("kw", "kilowatt", cat, THOUSAND);
        reg("hp", "horsepower", cat, HP_W);
        reg("btu/h", "BTU per hour", cat, BTU_H_W);
    }

    private static void registerDensity() {
        final UnitCategory cat = UnitCategory.DENSITY;
        regBase("kg/m3", "kilogram per cubic meter", cat);
        reg("g/cm3", "gram per cubic centimeter", cat, THOUSAND);
        reg("g/ml", "gram per milliliter", cat, THOUSAND);
        reg("lb/ft3", "pound per cubic foot", cat,
                new BigDecimal("16.018463374"));
    }

    private static void registerFrequency() {
        final UnitCategory cat = UnitCategory.FREQUENCY;
        regBase("hz", "hertz", cat);
        reg("khz", "kilohertz", cat, THOUSAND);
        reg("mhz", "megahertz", cat, new BigDecimal("1000000"));
        reg("ghz", "gigahertz", cat,
                new BigDecimal("1000000000"));
        reg("rpm", "revolutions per minute", cat, RPM_HZ);
    }

    private static void registerAngle() {
        final UnitCategory cat = UnitCategory.ANGLE;
        regBase("deg", "degree", cat);
        reg("rad", "radian", cat, DEG_PER_RAD);
        reg("grad", "gradian", cat, new BigDecimal("0.9"));
        reg("arcmin", "arcminute", cat, ARCMIN_DEG);
        reg("arcsec", "arcsecond", cat, ARCSEC_DEG);
        reg("turn", "turn", cat, new BigDecimal("360"));
    }

    private static void registerDataRate() {
        final UnitCategory cat = UnitCategory.DATA_RATE;
        regBase("bps", "bit per second", cat);
        reg("kbps", "kilobit per second", cat, THOUSAND);
        reg("mbps", "megabit per second", cat, MILLION);
        reg("gbps", "gigabit per second", cat, BILLION);
        reg("tbps", "terabit per second", cat, TRILLION);
        reg("byps", "byte per second", cat, EIGHT);
        reg("kbyps", "kilobyte per second", cat, new BigDecimal("8000"));
        reg("mbyps", "megabyte per second", cat, new BigDecimal("8000000"));
        reg("gbyps", "gigabyte per second", cat, new BigDecimal("8000000000"));
    }

    private static void registerResistance() {
        final UnitCategory cat = UnitCategory.RESISTANCE;
        regBase("ohm", "ohm", cat);
        reg("mohm", "milliohm", cat, MILLI);
        reg("kohm", "kiloohm", cat, THOUSAND);
        reg("megohm", "megaohm", cat, MILLION);
    }

    private static void registerCapacitance() {
        final UnitCategory cat = UnitCategory.CAPACITANCE;
        regBase("fd", "farad", cat);
        reg("mfd", "millifarad", cat, MILLI);
        reg("uf", "microfarad", cat, MICRO);
        reg("nf", "nanofarad", cat, NANO);
        reg("pf", "picofarad", cat, PICO);
    }

    private static void registerInductance() {
        final UnitCategory cat = UnitCategory.INDUCTANCE;
        regBase("hy", "henry", cat);
        reg("mhy", "millihenry", cat, MILLI);
        reg("uhy", "microhenry", cat, MICRO);
        reg("nhy", "nanohenry", cat, NANO);
    }

    private static void registerVoltage() {
        final UnitCategory cat = UnitCategory.VOLTAGE;
        regBase("vlt", "volt", cat);
        reg("mvlt", "millivolt", cat, MILLI);
        reg("kvlt", "kilovolt", cat, THOUSAND);
        reg("uvlt", "microvolt", cat, MICRO);
    }

    private static void registerCurrent() {
        final UnitCategory cat = UnitCategory.CURRENT;
        regBase("amp", "ampere", cat);
        reg("mamp", "milliampere", cat, MILLI);
        reg("uamp", "microampere", cat, MICRO);
        reg("namp", "nanoampere", cat, NANO);
    }

    private static void registerGasMark() {
        GAS_MARK_TO_C.put(1, new BigDecimal("140"));
        GAS_MARK_TO_C.put(2, new BigDecimal("150"));
        GAS_MARK_TO_C.put(3, new BigDecimal("170"));
        GAS_MARK_TO_C.put(4, new BigDecimal("180"));
        GAS_MARK_TO_C.put(5, new BigDecimal("190"));
        GAS_MARK_TO_C.put(6, new BigDecimal("200"));
        GAS_MARK_TO_C.put(7, new BigDecimal("220"));
        GAS_MARK_TO_C.put(8, new BigDecimal("230"));
        GAS_MARK_TO_C.put(9, new BigDecimal("240"));
        GAS_MARK_TO_C.put(10, new BigDecimal("260"));
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers
    // ------------------------------------------------------------------ //

    private static BigDecimal convertTemperature(
            final BigDecimal value,
            final String source, final String target) {

        final BigDecimal result;
        if (source.equals(target)) {
            result = value;
        } else {
            final BigDecimal celsius = toCelsius(source, value);
            result = fromCelsius(target, celsius);
        }
        return result;
    }

    private static UnitDefinition requireUnit(final String code) {
        final UnitDefinition def = UNITS.get(
                code.toLowerCase(Locale.ROOT));
        if (def == null) {
            throw new IllegalArgumentException(
                    "Unknown unit: " + code);
        }
        return def;
    }

    private static void requireSameCategory(
            final UnitDefinition source,
            final UnitDefinition target) {

        if (source.category() != target.category()) {
            throw new IllegalArgumentException(
                    "Cannot convert between %s (%s) and %s (%s)".formatted(
                            source.code(), source.category(),
                            target.code(), target.category()));
        }
    }

    private static String explainTemperature(
            final String source, final String target) {

        final String result;
        if (source.equals(target)) {
            result = "Same unit — no conversion needed";
        } else {
            result = TEMP_FORMULAS.getOrDefault(
                    source + "->" + target,
                    "Convert via Celsius intermediate");
        }
        return result;
    }

    private static String strip(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
