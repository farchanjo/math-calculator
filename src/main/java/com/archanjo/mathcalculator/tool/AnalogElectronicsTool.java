package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@SuppressWarnings("PMD.CyclomaticComplexity")
@Component
public class AnalogElectronicsTool {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int SCALE = 20;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal TWENTY = new BigDecimal("20");
    private static final BigDecimal TWO_PI =
            new BigDecimal("6.2831853071795864769252867665590057683943");
    private static final String SERIES = "series";
    private static final String PARALLEL = "parallel";
    private static final String ERROR_PREFIX = "Error: ";
    private static final int REQUIRED_KNOWNS = 2;
    private static final String JSON_CLOSE = "\"}";
    private static final String RES_OHMS_DESC = "Resistance in ohms";
    private static final String CAP_FARADS_DESC = "Capacitance in farads";

    @Tool(description = "Ohm's Law calculator. Given any 2 of V/I/R/P (non-empty), compute the other 2."
            + " Returns JSON with voltage, current, resistance, power.")
    public String ohmsLaw(
            @ToolParam(description = "Voltage in volts (empty if unknown)") final String voltage,
            @ToolParam(description = "Current in amps (empty if unknown)") final String current,
            @ToolParam(description = "Resistance in ohms (empty if unknown)") final String resistance,
            @ToolParam(description = "Power in watts (empty if unknown)") final String power) {
        String result;
        try {
            result = computeOhmsLaw(voltage, current, resistance, power);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute combined resistance. Series: sum. Parallel: reciprocal sum.")
    public String resistorCombination(
            @ToolParam(description = "Comma-separated resistance values in ohms") final String values,
            @ToolParam(description = "Mode: series or parallel") final String mode) {
        String result;
        try {
            final BigDecimal[] parsed = parseCsv(values);
            result = strip(combineSeries(parsed, mode));
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute combined capacitance. Series: reciprocal sum. Parallel: sum.")
    public String capacitorCombination(
            @ToolParam(description = "Comma-separated capacitance values in farads") final String values,
            @ToolParam(description = "Mode: series or parallel") final String mode) {
        String result;
        try {
            final BigDecimal[] parsed = parseCsv(values);
            result = strip(combineCapacitor(parsed, mode));
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute combined inductance. Series: sum. Parallel: reciprocal sum.")
    public String inductorCombination(
            @ToolParam(description = "Comma-separated inductance values in henrys") final String values,
            @ToolParam(description = "Mode: series or parallel") final String mode) {
        String result;
        try {
            final BigDecimal[] parsed = parseCsv(values);
            result = strip(combineSeries(parsed, mode));
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute voltage divider output. Vout = Vin * R2 / (R1 + R2).")
    public String voltageDivider(
            @ToolParam(description = "Input voltage in volts") final String vin,
            @ToolParam(description = "Resistor R1 in ohms") final String res1,
            @ToolParam(description = "Resistor R2 in ohms") final String res2) {
        String result;
        try {
            final BigDecimal vinVal = new BigDecimal(vin);
            final BigDecimal r1Val = new BigDecimal(res1);
            final BigDecimal r2Val = new BigDecimal(res2);
            final BigDecimal sum = r1Val.add(r2Val, PRECISION);
            validateNonZero(sum, "R1 + R2");
            final BigDecimal vout = vinVal.multiply(r2Val, PRECISION)
                    .divide(sum, SCALE, ROUNDING);
            result = strip(vout);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute current divider. I1 = It*R2/(R1+R2), I2 = It*R1/(R1+R2)."
            + " Returns JSON with i1 and i2.")
    public String currentDivider(
            @ToolParam(description = "Total current in amps") final String iTotal,
            @ToolParam(description = "Resistor R1 in ohms") final String res1,
            @ToolParam(description = "Resistor R2 in ohms") final String res2) {
        String result;
        try {
            final BigDecimal itVal = new BigDecimal(iTotal);
            final BigDecimal r1Val = new BigDecimal(res1);
            final BigDecimal r2Val = new BigDecimal(res2);
            final BigDecimal sum = r1Val.add(r2Val, PRECISION);
            validateNonZero(sum, "R1 + R2");
            final BigDecimal current1 = itVal.multiply(r2Val, PRECISION).divide(sum, SCALE, ROUNDING);
            final BigDecimal current2 = itVal.multiply(r1Val, PRECISION).divide(sum, SCALE, ROUNDING);
            result = "{\"i1\":\"" + strip(current1) + "\",\"i2\":\"" + strip(current2) + JSON_CLOSE;
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute RC time constant and cutoff frequency."
            + " Returns JSON with tau and cutoffFrequency.")
    public String rcTimeConstant(
            @ToolParam(description = RES_OHMS_DESC) final String resistance,
            @ToolParam(description = CAP_FARADS_DESC) final String capacitance) {
        String result;
        try {
            final BigDecimal rVal = new BigDecimal(resistance);
            final BigDecimal cVal = new BigDecimal(capacitance);
            final BigDecimal tau = rVal.multiply(cVal, PRECISION);
            validateNonZero(tau, "R*C");
            final BigDecimal freq = BigDecimal.ONE
                    .divide(TWO_PI.multiply(tau, PRECISION), SCALE, ROUNDING);
            result = "{\"tau\":\"" + strip(tau)
                    + "\",\"cutoffFrequency\":\"" + strip(freq) + JSON_CLOSE;
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute RL time constant and cutoff frequency."
            + " Returns JSON with tau and cutoffFrequency.")
    public String rlTimeConstant(
            @ToolParam(description = RES_OHMS_DESC) final String resistance,
            @ToolParam(description = "Inductance in henrys") final String inductance) {
        String result;
        try {
            final BigDecimal rVal = new BigDecimal(resistance);
            final BigDecimal lVal = new BigDecimal(inductance);
            validateNonZero(rVal, "Resistance");
            final BigDecimal tau = lVal.divide(rVal, SCALE, ROUNDING);
            final BigDecimal freq = rVal
                    .divide(TWO_PI.multiply(lVal, PRECISION), SCALE, ROUNDING);
            result = "{\"tau\":\"" + strip(tau)
                    + "\",\"cutoffFrequency\":\"" + strip(freq) + JSON_CLOSE;
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute RLC resonant frequency, Q factor, and bandwidth."
            + " Returns JSON.")
    public String rlcResonance(
            @ToolParam(description = RES_OHMS_DESC) final String resistance,
            @ToolParam(description = "Inductance in henrys") final String inductance,
            @ToolParam(description = CAP_FARADS_DESC) final String capacitance) {
        String result;
        try {
            final BigDecimal rVal = new BigDecimal(resistance);
            final BigDecimal lVal = new BigDecimal(inductance);
            final BigDecimal cVal = new BigDecimal(capacitance);
            final BigDecimal lcProduct = lVal.multiply(cVal, PRECISION);
            final double sqrtLc = StrictMath.sqrt(lcProduct.doubleValue());
            final BigDecimal resonantFreq = BigDecimal.ONE
                    .divide(TWO_PI.multiply(BigDecimal.valueOf(sqrtLc), PRECISION), SCALE, ROUNDING);
            final double impedanceRatio = StrictMath.sqrt(lVal.divide(cVal, SCALE, ROUNDING).doubleValue());
            final BigDecimal qFactor = BigDecimal.valueOf(impedanceRatio)
                    .divide(rVal, SCALE, ROUNDING);
            validateNonZero(qFactor, "Q factor");
            final BigDecimal bandwidth = resonantFreq.divide(qFactor, SCALE, ROUNDING);
            result = "{\"resonantFrequency\":\"" + strip(resonantFreq)
                    + "\",\"qFactor\":\"" + strip(qFactor)
                    + "\",\"bandwidth\":\"" + strip(bandwidth) + JSON_CLOSE;
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute series RLC impedance at a frequency."
            + " Returns JSON with magnitude and phase.")
    public String impedance(
            @ToolParam(description = RES_OHMS_DESC) final String resistance,
            @ToolParam(description = "Inductance in henrys") final String inductance,
            @ToolParam(description = CAP_FARADS_DESC) final String capacitance,
            @ToolParam(description = "Frequency in hertz") final String freqHz) {
        String result;
        try {
            final BigDecimal rVal = new BigDecimal(resistance);
            final BigDecimal lVal = new BigDecimal(inductance);
            final BigDecimal cVal = new BigDecimal(capacitance);
            final BigDecimal freq = new BigDecimal(freqHz);
            final BigDecimal omega = TWO_PI.multiply(freq, PRECISION);
            final BigDecimal xInductive = omega.multiply(lVal, PRECISION);
            final BigDecimal xCapacitive = BigDecimal.ONE
                    .divide(omega.multiply(cVal, PRECISION), SCALE, ROUNDING);
            final BigDecimal reactance = xInductive.subtract(xCapacitive, PRECISION);
            final double rDouble = rVal.doubleValue();
            final double xDouble = reactance.doubleValue();
            final double magnitude = StrictMath.sqrt(rDouble * rDouble + xDouble * xDouble);
            final double phaseDeg = StrictMath.toDegrees(StrictMath.atan2(xDouble, rDouble));
            result = "{\"magnitude\":\"" + strip(BigDecimal.valueOf(magnitude))
                    + "\",\"phase\":\"" + strip(BigDecimal.valueOf(phaseDeg)) + JSON_CLOSE;
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Convert between decibels and linear ratios."
            + " Modes: powerToDb, voltageToDb, dbToPower, dbToVoltage.")
    public String decibelConvert(
            @ToolParam(description = "Numeric value") final String value,
            @ToolParam(description = "Mode: powerToDb, voltageToDb, dbToPower, dbToVoltage")
            final String mode) {
        String result;
        try {
            final BigDecimal val = new BigDecimal(value);
            result = strip(computeDecibel(val, mode));
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute RC filter cutoff frequency. fc = 1/(2*pi*R*C)."
            + " Returns JSON with cutoffFrequency and filterType.")
    public String filterCutoff(
            @ToolParam(description = RES_OHMS_DESC) final String resistance,
            @ToolParam(description = CAP_FARADS_DESC) final String capacitance,
            @ToolParam(description = "Filter type: lowpass or highpass") final String filterType) {
        String result;
        try {
            final BigDecimal rVal = new BigDecimal(resistance);
            final BigDecimal cVal = new BigDecimal(capacitance);
            final BigDecimal rcProduct = rVal.multiply(cVal, PRECISION);
            validateNonZero(rcProduct, "R*C");
            final BigDecimal freq = BigDecimal.ONE
                    .divide(TWO_PI.multiply(rcProduct, PRECISION), SCALE, ROUNDING);
            final String type = validateFilterType(filterType);
            result = "{\"cutoffFrequency\":\"" + strip(freq)
                    + "\",\"filterType\":\"" + type + JSON_CLOSE;
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute LED current-limiting resistor. R = (Vs - Vf) / If.")
    public String ledResistor(
            @ToolParam(description = "Supply voltage in volts") final String supplyVoltage,
            @ToolParam(description = "LED forward voltage in volts") final String forwardVoltage,
            @ToolParam(description = "LED forward current in amps") final String forwardCurrent) {
        String result;
        try {
            final BigDecimal vsVal = new BigDecimal(supplyVoltage);
            final BigDecimal vfVal = new BigDecimal(forwardVoltage);
            final BigDecimal ifVal = new BigDecimal(forwardCurrent);
            result = computeLedResistor(vsVal, vfVal, ifVal);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compute Wheatstone bridge balance resistor. R4 = R3 * R2 / R1.")
    public String wheatstoneBridge(
            @ToolParam(description = "Known resistor R1 in ohms") final String res1,
            @ToolParam(description = "Known resistor R2 in ohms") final String res2,
            @ToolParam(description = "Known resistor R3 in ohms") final String res3) {
        String result;
        try {
            final BigDecimal r1Val = new BigDecimal(res1);
            final BigDecimal r2Val = new BigDecimal(res2);
            final BigDecimal r3Val = new BigDecimal(res3);
            validateNonZero(r1Val, "R1");
            final BigDecimal r4Val = r3Val.multiply(r2Val, PRECISION)
                    .divide(r1Val, SCALE, ROUNDING);
            result = strip(r4Val);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    // --- Ohm's Law helpers ---

    private String computeOhmsLaw(final String volt, final String curr,
                                  final String res, final String pow) {
        final boolean hasV = isPresent(volt);
        final boolean hasI = isPresent(curr);
        final boolean hasR = isPresent(res);
        final boolean hasP = isPresent(pow);
        final int count = boolToInt(hasV) + boolToInt(hasI)
                + boolToInt(hasR) + boolToInt(hasP);
        if (count != REQUIRED_KNOWNS) {
            throw new IllegalArgumentException(
                    "Exactly 2 of V, I, R, P must be provided. Got: " + count);
        }
        return dispatchOhms(hasV, hasI, hasR, volt, curr, res, pow);
    }

    private String dispatchOhms(final boolean hasV, final boolean hasI,
                                final boolean hasR,
                                final String volt, final String curr,
                                final String res, final String pow) {
        final int key = (hasV ? 4 : 0) | (hasI ? 2 : 0) | (hasR ? 1 : 0);
        return switch (key) {
            case 6 -> ohmsFromVI(new BigDecimal(volt), new BigDecimal(curr));
            case 5 -> ohmsFromVR(new BigDecimal(volt), new BigDecimal(res));
            case 4 -> ohmsFromVP(new BigDecimal(volt), new BigDecimal(pow));
            case 3 -> ohmsFromIR(new BigDecimal(curr), new BigDecimal(res));
            case 2 -> ohmsFromIP(new BigDecimal(curr), new BigDecimal(pow));
            default -> ohmsFromRP(new BigDecimal(res), new BigDecimal(pow));
        };
    }

    private String ohmsFromVI(final BigDecimal voltage, final BigDecimal current) {
        validateNonZero(current, "Current");
        final BigDecimal resistance = voltage.divide(current, SCALE, ROUNDING);
        final BigDecimal power = voltage.multiply(current, PRECISION);
        return ohmsJson(voltage, current, resistance, power);
    }

    private String ohmsFromVR(final BigDecimal voltage, final BigDecimal resistance) {
        validateNonZero(resistance, "Resistance");
        final BigDecimal current = voltage.divide(resistance, SCALE, ROUNDING);
        final BigDecimal power = voltage.multiply(current, PRECISION);
        return ohmsJson(voltage, current, resistance, power);
    }

    private String ohmsFromVP(final BigDecimal voltage, final BigDecimal power) {
        validateNonZero(voltage, "Voltage");
        final BigDecimal current = power.divide(voltage, SCALE, ROUNDING);
        final BigDecimal resistance = voltage.divide(current, SCALE, ROUNDING);
        return ohmsJson(voltage, current, resistance, power);
    }

    private String ohmsFromIR(final BigDecimal current, final BigDecimal resistance) {
        final BigDecimal voltage = current.multiply(resistance, PRECISION);
        final BigDecimal power = voltage.multiply(current, PRECISION);
        return ohmsJson(voltage, current, resistance, power);
    }

    private String ohmsFromIP(final BigDecimal current, final BigDecimal power) {
        validateNonZero(current, "Current");
        final BigDecimal voltage = power.divide(current, SCALE, ROUNDING);
        final BigDecimal resistance = voltage.divide(current, SCALE, ROUNDING);
        return ohmsJson(voltage, current, resistance, power);
    }

    private String ohmsFromRP(final BigDecimal resistance, final BigDecimal power) {
        final double prProduct = power.multiply(resistance, PRECISION).doubleValue();
        final BigDecimal voltage = BigDecimal.valueOf(StrictMath.sqrt(prProduct));
        final double prRatio = power.divide(resistance, SCALE, ROUNDING).doubleValue();
        final BigDecimal current = BigDecimal.valueOf(StrictMath.sqrt(prRatio));
        return ohmsJson(voltage, current, resistance, power);
    }

    private String ohmsJson(final BigDecimal voltage, final BigDecimal current,
                            final BigDecimal resistance, final BigDecimal power) {
        return "{\"voltage\":\"" + strip(voltage)
                + "\",\"current\":\"" + strip(current)
                + "\",\"resistance\":\"" + strip(resistance)
                + "\",\"power\":\"" + strip(power) + JSON_CLOSE;
    }

    // --- Combination helpers ---

    private BigDecimal combineSeries(final BigDecimal[] vals, final String mode) {
        final BigDecimal result;
        if (SERIES.equalsIgnoreCase(mode)) {
            result = sumValues(vals);
        } else if (PARALLEL.equalsIgnoreCase(mode)) {
            result = reciprocalSum(vals);
        } else {
            throw new IllegalArgumentException("Mode must be 'series' or 'parallel'");
        }
        return result;
    }

    private BigDecimal combineCapacitor(final BigDecimal[] vals, final String mode) {
        final BigDecimal result;
        if (SERIES.equalsIgnoreCase(mode)) {
            result = reciprocalSum(vals);
        } else if (PARALLEL.equalsIgnoreCase(mode)) {
            result = sumValues(vals);
        } else {
            throw new IllegalArgumentException("Mode must be 'series' or 'parallel'");
        }
        return result;
    }

    private BigDecimal sumValues(final BigDecimal... vals) {
        BigDecimal total = BigDecimal.ZERO;
        for (final BigDecimal val : vals) {
            total = total.add(val, PRECISION);
        }
        return total;
    }

    private BigDecimal reciprocalSum(final BigDecimal... vals) {
        BigDecimal reciprocal = BigDecimal.ZERO;
        for (final BigDecimal val : vals) {
            validateNonZero(val, "Component value");
            reciprocal = reciprocal.add(
                    BigDecimal.ONE.divide(val, SCALE, ROUNDING), PRECISION);
        }
        validateNonZero(reciprocal, "Reciprocal sum");
        return BigDecimal.ONE.divide(reciprocal, SCALE, ROUNDING);
    }

    // --- LED helper ---

    private String computeLedResistor(final BigDecimal vsVal,
                                      final BigDecimal vfVal,
                                      final BigDecimal ifVal) {
        final String result;
        if (vsVal.compareTo(vfVal) <= 0) {
            result = ERROR_PREFIX
                    + "Supply voltage must be greater than forward voltage";
        } else if (ifVal.compareTo(BigDecimal.ZERO) <= 0) {
            result = ERROR_PREFIX
                    + "Forward current must be greater than zero";
        } else {
            result = strip(vsVal.subtract(vfVal, PRECISION)
                    .divide(ifVal, SCALE, ROUNDING));
        }
        return result;
    }

    // --- Decibel helper ---

    private BigDecimal computeDecibel(final BigDecimal val, final String mode) {
        return switch (mode) {
            case "powerToDb" -> {
                validatePositive(val, "Power value");
                yield TEN.multiply(
                        BigDecimal.valueOf(StrictMath.log10(val.doubleValue())), PRECISION);
            }
            case "voltageToDb" -> {
                validatePositive(val, "Voltage value");
                yield TWENTY.multiply(
                        BigDecimal.valueOf(StrictMath.log10(val.doubleValue())), PRECISION);
            }
            case "dbToPower" -> {
                final BigDecimal exponent = val.divide(TEN, SCALE, ROUNDING);
                yield BigDecimal.valueOf(StrictMath.pow(TEN.doubleValue(), exponent.doubleValue()));
            }
            case "dbToVoltage" -> {
                final BigDecimal exponent = val.divide(TWENTY, SCALE, ROUNDING);
                yield BigDecimal.valueOf(StrictMath.pow(TEN.doubleValue(), exponent.doubleValue()));
            }
            default -> throw new IllegalArgumentException(
                    "Mode must be powerToDb, voltageToDb, dbToPower, or dbToVoltage");
        };
    }

    // --- Parsing and validation helpers ---

    private BigDecimal[] parseCsv(final String values) {
        final String[] parts = values.split(",");
        if (parts.length == 0) {
            throw new IllegalArgumentException("At least one value is required");
        }
        final BigDecimal[] result = new BigDecimal[parts.length];
        for (int idx = 0; idx < parts.length; idx++) {
            result[idx] = new BigDecimal(parts[idx].trim());
        }
        return result;
    }

    private boolean isPresent(final String value) {
        return value != null && !value.isEmpty();
    }

    private int boolToInt(final boolean flag) {
        return flag ? 1 : 0;
    }

    private String validateFilterType(final String filterType) {
        final String lower = filterType.toLowerCase(Locale.ROOT);
        if (!"lowpass".equals(lower) && !"highpass".equals(lower)) {
            throw new IllegalArgumentException(
                    "Filter type must be 'lowpass' or 'highpass'");
        }
        return lower;
    }

    private void validateNonZero(final BigDecimal value, final String name) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(name + " must not be zero");
        }
    }

    private void validatePositive(final BigDecimal value, final String name) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private String strip(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
