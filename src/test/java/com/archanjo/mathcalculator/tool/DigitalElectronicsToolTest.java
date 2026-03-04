package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DigitalElectronicsToolTest {

    private static final String ERROR_PREFIX = "Error:";
    private static final String DECIMAL_TEN = "10";
    private static final String DECIMAL_TWELVE = "12";
    private static final String HEX_VALUE_FF = "FF";
    private static final String BINARY_11111111 = "11111111";
    private static final String VREF_FIVE = "5";
    private static final String RESISTANCE_1K = "1000";
    private static final String CAPACITANCE_1UF = "0.000001";
    private static final String DIR_TO_TWOS = "toTwos";
    private static final String DIR_FROM_TWOS = "fromTwos";
    private static final String DIR_TO_GRAY = "toGray";
    private static final String DIR_FROM_GRAY = "fromGray";
    private static final String FREQ_TO_PERIOD = "freqToPeriod";
    private static final String PERIOD_TO_FREQ = "periodToFreq";
    private static final int BASE_TWO = 2;
    private static final int BASE_TEN = 10;
    private static final int BASE_SIXTEEN = 16;
    private static final int EIGHT_BITS = 8;

    private final DigitalElectronicsTool tool = new DigitalElectronicsTool();

    @Nested
    @DisplayName("convertBase")
    class ConvertBaseTests {

        @Test
        @DisplayName("255 decimal to hex should produce FF")
        void decimalToHex() {
            final String result = tool.convertBase("255", BASE_TEN, BASE_SIXTEEN);
            assertEquals(HEX_VALUE_FF, result,
                    "Converting 255 from base 10 to base 16 should produce FF");
        }

        @Test
        @DisplayName("FF hex to binary should produce 11111111")
        void hexToBinary() {
            final String result = tool.convertBase(HEX_VALUE_FF, BASE_SIXTEEN, BASE_TWO);
            assertEquals(BINARY_11111111, result,
                    "Converting FF from base 16 to base 2 should produce 11111111");
        }

        @Test
        @DisplayName("10 binary to decimal should produce 2")
        void binaryToDecimal() {
            final String result = tool.convertBase(DECIMAL_TEN, BASE_TWO, BASE_TEN);
            assertEquals("2", result,
                    "Converting 10 from base 2 to base 10 should produce 2");
        }
    }

    @Nested
    @DisplayName("twosComplement")
    class TwosComplementTests {

        @Test
        @DisplayName("-1 in 8-bit toTwos should produce 11111111")
        void negativeOneToTwos() {
            final String result = tool.twosComplement("-1", EIGHT_BITS, DIR_TO_TWOS);
            assertEquals(BINARY_11111111, result,
                    "Two's complement of -1 in 8 bits should be 11111111");
        }

        @Test
        @DisplayName("11111111 in 8-bit fromTwos should produce -1")
        void fromTwosNegativeOne() {
            final String result = tool.twosComplement(BINARY_11111111, EIGHT_BITS,
                    DIR_FROM_TWOS);
            assertEquals("-1", result,
                    "Decoding 11111111 in 8-bit two's complement should produce -1");
        }

        @Test
        @DisplayName("positive value roundtrip through toTwos and fromTwos")
        void positiveRoundtrip() {
            final String encoded = tool.twosComplement("42", EIGHT_BITS, DIR_TO_TWOS);
            final String decoded = tool.twosComplement(encoded, EIGHT_BITS, DIR_FROM_TWOS);
            assertEquals("42", decoded,
                    "Encoding then decoding a positive value should return the original");
        }
    }

    @Nested
    @DisplayName("grayCode")
    class GrayCodeTests {

        @Test
        @DisplayName("binary 0100 to Gray should produce 0110")
        void binaryToGray() {
            final String result = tool.grayCode("0100", DIR_TO_GRAY);
            assertEquals("0110", result,
                    "Binary 0100 converted to Gray code should produce 0110");
        }

        @Test
        @DisplayName("Gray code roundtrip should return original binary")
        void grayRoundtrip() {
            final String grayEncoded = tool.grayCode("1011", DIR_TO_GRAY);
            final String decoded = tool.grayCode(grayEncoded, DIR_FROM_GRAY);
            assertEquals("1011", decoded,
                    "Converting to Gray and back should return the original binary");
        }
    }

    @Nested
    @DisplayName("bitwiseOp")
    class BitwiseOpTests {

        @Test
        @DisplayName("AND of 12 and 10 should produce 8")
        void bitwiseAnd() {
            final String result = tool.bitwiseOp(DECIMAL_TWELVE, DECIMAL_TEN, "AND");
            assertTrue(result.contains("\"decimal\":\"8\""),
                    "Bitwise AND of 12 and 10 should produce decimal 8");
        }

        @Test
        @DisplayName("OR of 12 and 10 should produce 14")
        void bitwiseOr() {
            final String result = tool.bitwiseOp(DECIMAL_TWELVE, DECIMAL_TEN, "OR");
            assertTrue(result.contains("\"decimal\":\"14\""),
                    "Bitwise OR of 12 and 10 should produce decimal 14");
        }

        @Test
        @DisplayName("XOR of 12 and 10 should produce 6")
        void bitwiseXor() {
            final String result = tool.bitwiseOp(DECIMAL_TWELVE, DECIMAL_TEN, "XOR");
            assertTrue(result.contains("\"decimal\":\"6\""),
                    "Bitwise XOR of 12 and 10 should produce decimal 6");
        }

        @Test
        @DisplayName("NOT of 0 should produce -1")
        void bitwiseNot() {
            final String result = tool.bitwiseOp("0", "0", "NOT");
            assertTrue(result.contains("\"decimal\":\"-1\""),
                    "Bitwise NOT of 0 should produce decimal -1");
        }
    }

    @Nested
    @DisplayName("adcResolution")
    class AdcResolutionTests {

        @Test
        @DisplayName("8-bit ADC with Vref 5V should have LSB approximately 0.01953")
        void eightBitAdcLsb() {
            final String result = tool.adcResolution(EIGHT_BITS, VREF_FIVE);
            assertTrue(result.contains("\"lsb\":\"0.01953"),
                    "8-bit ADC with 5V Vref should have LSB starting with 0.01953");
        }
    }

    @Nested
    @DisplayName("dacOutput")
    class DacOutputTests {

        @Test
        @DisplayName("8-bit DAC with Vref 5V and code 128 should output 2.5V")
        void eightBitDacMidpoint() {
            final String result = tool.dacOutput(EIGHT_BITS, VREF_FIVE, 128);
            assertEquals("2.5", result,
                    "8-bit DAC with Vref=5 and code=128 should output 2.5V");
        }
    }

    @Nested
    @DisplayName("timer555Astable")
    class Timer555AstableTests {

        @Test
        @DisplayName("R1=1000 R2=1000 C=1uF should produce valid frequency and duty cycle")
        void astableWithStandardValues() {
            final String result = tool.timer555Astable(RESISTANCE_1K, RESISTANCE_1K,
                    CAPACITANCE_1UF);
            assertTrue(result.contains("\"frequency\":\""),
                    "Astable output should contain a frequency field");
        }
    }

    @Nested
    @DisplayName("timer555Monostable")
    class Timer555MonostableTests {

        @Test
        @DisplayName("R=1000 C=1uF should produce pulse width approximately 0.0011")
        void monostableStandardValues() {
            final String result = tool.timer555Monostable(RESISTANCE_1K, CAPACITANCE_1UF);
            assertTrue(result.contains("\"pulseWidth\":\"0.0011\""),
                    "Monostable pulse width with R=1000 and C=1uF should be 0.0011");
        }
    }

    @Nested
    @DisplayName("frequencyPeriod")
    class FrequencyPeriodTests {

        @Test
        @DisplayName("freqToPeriod of 1000 Hz should produce 0.001 s")
        void frequencyToPeriod() {
            final String result = tool.frequencyPeriod("1000", FREQ_TO_PERIOD);
            assertEquals("0.001", result,
                    "Converting 1000 Hz to period should produce 0.001 seconds");
        }

        @Test
        @DisplayName("periodToFreq of 0.001 s should produce 1000 Hz")
        void periodToFrequency() {
            final String result = tool.frequencyPeriod("0.001", PERIOD_TO_FREQ);
            assertEquals("1000", result,
                    "Converting 0.001 seconds to frequency should produce 1000 Hz");
        }
    }

    @Nested
    @DisplayName("nyquistRate")
    class NyquistRateTests {

        @Test
        @DisplayName("20000 Hz bandwidth should require 40000 Hz sample rate")
        void nyquistDoublesBandwidth() {
            final String result = tool.nyquistRate("20000");
            assertTrue(result.contains("\"minSampleRate\":\"40000\""),
                    "Nyquist rate for 20000 Hz should be 40000 Hz");
        }
    }

    @Nested
    @DisplayName("error cases")
    class ErrorCaseTests {

        @Test
        @DisplayName("convertBase with invalid source base should return error")
        void invalidSourceBase() {
            final String result = tool.convertBase("10", 1, BASE_TEN);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Base below 2 should produce an error message");
        }

        @Test
        @DisplayName("convertBase with invalid target base should return error")
        void invalidTargetBase() {
            final String result = tool.convertBase("10", BASE_TEN, 37);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Base above 36 should produce an error message");
        }

        @Test
        @DisplayName("twosComplement with invalid direction should return error")
        void invalidTwosDirection() {
            final String result = tool.twosComplement("5", EIGHT_BITS, "invalid");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid direction for twosComplement should produce an error message");
        }

        @Test
        @DisplayName("grayCode with invalid direction should return error")
        void invalidGrayDirection() {
            final String result = tool.grayCode("0100", "invalid");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid direction for grayCode should produce an error message");
        }

        @Test
        @DisplayName("frequencyPeriod with zero value should return error")
        void frequencyPeriodZeroValue() {
            final String result = tool.frequencyPeriod("0", FREQ_TO_PERIOD);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Zero value for frequencyPeriod should produce an error message");
        }

        @Test
        @DisplayName("bitwiseOp with unknown operation should return error")
        void unknownBitwiseOperation() {
            final String result = tool.bitwiseOp("5", "3", "NAND");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Unknown bitwise operation should produce an error message");
        }

        @Test
        @DisplayName("dacOutput with out-of-range code should return error")
        void dacOutputCodeOutOfRange() {
            final String result = tool.dacOutput(EIGHT_BITS, VREF_FIVE, 256);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "DAC code exceeding max value should produce an error message");
        }

        @Test
        @DisplayName("twosComplement with zero bit width should return error")
        void twosComplementZeroBitWidth() {
            final String result = tool.twosComplement("5", 0, DIR_TO_TWOS);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Bit width of 0 should produce an error message");
        }
    }
}
