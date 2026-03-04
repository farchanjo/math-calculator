package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DigitalElectronicsTool {

    private static final String ERROR_PREFIX = "Error: ";
    private static final String TO_TWOS = "toTwos";
    private static final String FROM_TWOS = "fromTwos";
    private static final String TO_GRAY = "toGray";
    private static final String FROM_GRAY = "fromGray";
    private static final String JSON_CLOSE = "\"}";
    private static final BigDecimal LN2_RECIPROCAL =
            new BigDecimal("1.44269504088896340735992468100189213742665");
    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int SCALE = 20;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int MIN_BASE = 2;
    private static final int MAX_BASE = 36;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TIMER_CONSTANT = new BigDecimal("1.1");
    private static final int MAX_BITS = 64;

    @Tool(description = "Convert a number between any bases (2-36).")
    public String convertBase(
            @ToolParam(description = "Number string in source base") final String number,
            @ToolParam(description = "Source base (2-36)") final int fromBase,
            @ToolParam(description = "Target base (2-36)") final int toBase) {
        final String fromError = checkBase(fromBase);
        final String toError = fromError == null ? checkBase(toBase) : fromError;
        final String result;
        if (toError != null) {
            result = toError;
        } else {
            result = new BigInteger(number, fromBase).toString(toBase).toUpperCase(Locale.ROOT);
        }
        return result;
    }

    @Tool(description = "Compute or decode two's complement representation.")
    public String twosComplement(
            @ToolParam(description = "Decimal value (toTwos) or binary string (fromTwos)")
            final String value,
            @ToolParam(description = "Bit width (1-64)") final int bits,
            @ToolParam(description = "Direction: 'toTwos' or 'fromTwos'") final String direction) {
        final String bitsError = checkBitWidth(bits);
        final String result;
        if (bitsError != null) {
            result = bitsError;
        } else if (TO_TWOS.equals(direction)) {
            result = encodeToTwos(value, bits);
        } else if (FROM_TWOS.equals(direction)) {
            result = decodeFromTwos(value, bits);
        } else {
            result = ERROR_PREFIX + "Direction must be '" + TO_TWOS + "' or '" + FROM_TWOS + "'";
        }
        return result;
    }

    @Tool(description = "Convert between binary and Gray code.")
    public String grayCode(
            @ToolParam(description = "Binary string") final String binary,
            @ToolParam(description = "Direction: 'toGray' or 'fromGray'") final String direction) {
        final int width = binary.length();
        final String result;
        if (TO_GRAY.equals(direction)) {
            result = encodeBinaryToGray(binary, width);
        } else if (FROM_GRAY.equals(direction)) {
            result = decodeGrayToBinary(binary, width);
        } else {
            result = ERROR_PREFIX + "Direction must be '" + TO_GRAY + "' or '" + FROM_GRAY + "'";
        }
        return result;
    }

    @Tool(description = "Perform bitwise operation: AND, OR, XOR, NOT, SHL, SHR.")
    public String bitwiseOp(
            @ToolParam(description = "First operand (decimal)") final String operandA,
            @ToolParam(description = "Second operand (decimal) or shift count")
            final String operandB,
            @ToolParam(description = "Operation: AND, OR, XOR, NOT, SHL, SHR")
            final String operation) {
        final BigInteger valA = new BigInteger(operandA);
        return executeBitwiseOp(valA, operandB, operation.toUpperCase(Locale.ROOT));
    }

    @Tool(description = "Compute ADC resolution: LSB voltage and step count.")
    public String adcResolution(
            @ToolParam(description = "Number of bits") final int bits,
            @ToolParam(description = "Reference voltage") final String vref) {
        final BigDecimal vrefDec = new BigDecimal(vref);
        final BigDecimal levels = BigDecimal.TWO.pow(bits, PRECISION);
        final BigDecimal lsb = vrefDec.divide(levels, SCALE, ROUNDING);
        final BigDecimal stepCount = levels.subtract(BigDecimal.ONE);
        return "{\"lsb\":\"" + strip(lsb)
                + "\",\"stepCount\":\"" + strip(stepCount)
                + "\",\"bits\":" + bits + "}";
    }

    @Tool(description = "Compute DAC output voltage for a given digital code.")
    public String dacOutput(
            @ToolParam(description = "Number of bits") final int bits,
            @ToolParam(description = "Reference voltage") final String vref,
            @ToolParam(description = "Digital code (0 to 2^bits - 1)") final long code) {
        final long maxCode = (1L << bits) - 1;
        final String result;
        if (code < 0 || code > maxCode) {
            result = ERROR_PREFIX + "Code must be between 0 and " + maxCode;
        } else {
            final BigDecimal vrefDec = new BigDecimal(vref);
            final BigDecimal levels = BigDecimal.TWO.pow(bits, PRECISION);
            final BigDecimal vout = vrefDec
                    .multiply(BigDecimal.valueOf(code), PRECISION)
                    .divide(levels, SCALE, ROUNDING);
            result = strip(vout);
        }
        return result;
    }

    @Tool(description = "Calculate 555 timer astable mode: frequency, duty cycle, period.")
    public String timer555Astable(
            @ToolParam(description = "Resistance R1 in ohms") final String resistance1,
            @ToolParam(description = "Resistance R2 in ohms") final String resistance2,
            @ToolParam(description = "Capacitance in farads") final String capacitance) {
        final BigDecimal r1Dec = new BigDecimal(resistance1);
        final BigDecimal r2Dec = new BigDecimal(resistance2);
        final BigDecimal cDec = new BigDecimal(capacitance);
        final BigDecimal r1Plus2R2 = r1Dec.add(r2Dec.multiply(BigDecimal.TWO, PRECISION), PRECISION);
        final BigDecimal denominator = r1Plus2R2.multiply(cDec, PRECISION);
        final BigDecimal freq = LN2_RECIPROCAL.divide(denominator, SCALE, ROUNDING);
        final BigDecimal period = BigDecimal.ONE.divide(freq, SCALE, ROUNDING);
        final BigDecimal r1PlusR2 = r1Dec.add(r2Dec, PRECISION);
        final BigDecimal duty = r1PlusR2
                .divide(r1Plus2R2, SCALE, ROUNDING)
                .multiply(HUNDRED, PRECISION);
        return "{\"frequency\":\"" + strip(freq)
                + "\",\"dutyCycle\":\"" + strip(duty)
                + "\",\"period\":\"" + strip(period) + JSON_CLOSE;
    }

    @Tool(description = "Calculate 555 timer monostable mode: pulse width.")
    public String timer555Monostable(
            @ToolParam(description = "Resistance in ohms") final String resistance,
            @ToolParam(description = "Capacitance in farads") final String capacitance) {
        final BigDecimal rDec = new BigDecimal(resistance);
        final BigDecimal cDec = new BigDecimal(capacitance);
        final BigDecimal pulseWidth = TIMER_CONSTANT
                .multiply(rDec, PRECISION)
                .multiply(cDec, PRECISION);
        return "{\"pulseWidth\":\"" + strip(pulseWidth) + JSON_CLOSE;
    }

    @Tool(description = "Convert between frequency and period.")
    public String frequencyPeriod(
            @ToolParam(description = "Frequency in Hz or period in seconds") final String value,
            @ToolParam(description = "Mode: 'freqToPeriod' or 'periodToFreq'") final String mode) {
        final BigDecimal val = new BigDecimal(value);
        final String result;
        if (val.compareTo(BigDecimal.ZERO) <= 0) {
            result = ERROR_PREFIX + "Value must be positive";
        } else {
            result = strip(BigDecimal.ONE.divide(val, SCALE, ROUNDING));
        }
        return result;
    }

    @Tool(description = "Compute Nyquist rate: minimum sampling rate for a given bandwidth.")
    public String nyquistRate(
            @ToolParam(description = "Signal bandwidth in Hz") final String bandwidthHz) {
        final BigDecimal bandwidth = new BigDecimal(bandwidthHz);
        final BigDecimal minSampleRate = bandwidth.multiply(BigDecimal.TWO, PRECISION);
        return "{\"minSampleRate\":\"" + strip(minSampleRate)
                + "\",\"bandwidth\":\"" + strip(bandwidth) + JSON_CLOSE;
    }

    private String encodeToTwos(final String value, final int bits) {
        final long val = Long.parseLong(value);
        final long mask = (bits == MAX_BITS) ? -1L : (1L << bits) - 1;
        final long twos = val & mask;
        return padBinary(Long.toBinaryString(twos), bits);
    }

    private String decodeFromTwos(final String value, final int bits) {
        final long parsed = Long.parseUnsignedLong(value, MIN_BASE);
        final boolean msb = value.charAt(0) == '1' && value.length() == bits;
        final long result;
        if (msb) {
            final long mask = (bits == MAX_BITS) ? 0L : 1L << bits;
            result = parsed - mask;
        } else {
            result = parsed;
        }
        return String.valueOf(result);
    }

    private String encodeBinaryToGray(final String binary, final int width) {
        final long num = Long.parseUnsignedLong(binary, MIN_BASE);
        final long gray = num ^ (num >>> 1);
        return padBinary(Long.toBinaryString(gray), width);
    }

    private String decodeGrayToBinary(final String binary, final int width) {
        long num = Long.parseUnsignedLong(binary, MIN_BASE);
        long mask = num >>> 1;
        while (mask != 0) {
            num ^= mask;
            mask >>>= 1;
        }
        return padBinary(Long.toBinaryString(num), width);
    }

    private String executeBitwiseOp(final BigInteger valA, final String operandB,
                                    final String operation) {
        final BigInteger computed = computeBitwise(valA, operandB, operation);
        final String result;
        if (computed == null) {
            result = ERROR_PREFIX + "Unknown operation: " + operation;
        } else {
            result = "{\"decimal\":\"" + computed
                    + "\",\"binary\":\"" + computed.toString(MIN_BASE) + JSON_CLOSE;
        }
        return result;
    }

    private BigInteger computeBitwise(final BigInteger valA, final String operandB,
                                      final String operation) {
        final BigInteger computed;
        switch (operation) {
            case "AND" -> computed = valA.and(new BigInteger(operandB));
            case "OR" -> computed = valA.or(new BigInteger(operandB));
            case "XOR" -> computed = valA.xor(new BigInteger(operandB));
            case "NOT" -> computed = valA.not();
            case "SHL" -> computed = valA.shiftLeft(Integer.parseInt(operandB));
            case "SHR" -> computed = valA.shiftRight(Integer.parseInt(operandB));
            default -> computed = null;
        }
        return computed;
    }

    private String checkBase(final int base) {
        String error = null;
        if (base < MIN_BASE || base > MAX_BASE) {
            error = ERROR_PREFIX + "Base must be between " + MIN_BASE + " and " + MAX_BASE;
        }
        return error;
    }

    private String checkBitWidth(final int bits) {
        String error = null;
        if (bits < 1 || bits > MAX_BITS) {
            error = ERROR_PREFIX + "Bit width must be between 1 and " + MAX_BITS;
        }
        return error;
    }

    private String padBinary(final String binary, final int width) {
        final String result;
        if (binary.length() >= width) {
            result = binary;
        } else {
            result = "0".repeat(width - binary.length()) + binary;
        }
        return result;
    }

    private String strip(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
