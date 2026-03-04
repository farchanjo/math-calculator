package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnalogElectronicsToolTest {

    private static final BigDecimal TOLERANCE = BigDecimal.valueOf(1, 1);
    private static final BigDecimal EXPECTED_TWO = BigDecimal.TWO;
    private static final BigDecimal EXPECTED_CUTOFF = new BigDecimal("159.15");
    private static final BigDecimal EXPECTED_TAU = new BigDecimal("0.001");
    private static final String SERIES = "series";
    private static final String PARALLEL = "parallel";
    private static final String ERROR_PREFIX = "Error: ";
    private static final String EMPTY_PARAM = "";
    private static final String LOWPASS = "lowpass";
    private static final String POWER_TO_DB = "powerToDb";
    private static final String DB_TO_POWER = "dbToPower";
    private static final String VOLTAGE_TO_DB = "voltageToDb";
    private static final String DB_TO_VOLTAGE = "dbToVoltage";
    private static final String VALUE_100 = "100";
    private static final String VALUE_200 = "200";
    private static final String VALUE_300 = "300";
    private static final String VALUE_1000 = "1000";
    private static final String VALUE_2000 = "2000";
    private static final String ONE_MICROFARAD = "0.000001";
    private static final String INDUCTANCE_1MH = "0.001";
    private static final String JSON_KEY_TAU = "tau";
    private static final String JSON_KEY_CUTOFF = "cutoffFrequency";

    private final AnalogElectronicsTool tool = new AnalogElectronicsTool();

    private void assertClose(
            final BigDecimal expected, final String actual,
            final String msg) {
        final BigDecimal actualVal = new BigDecimal(actual);
        final BigDecimal diff = expected.subtract(actualVal).abs();
        assertTrue(diff.compareTo(TOLERANCE) < 0,
                msg + " -- expected: " + expected
                        + ", actual: " + actual);
    }

    @Nested
    @DisplayName("ohmsLaw")
    class OhmsLawTests {

        @Test
        void givenVoltageAndResistanceReturnsCurrent() {
            final String result = tool.ohmsLaw(
                    "12", EMPTY_PARAM, "4", EMPTY_PARAM);
            assertTrue(result.contains("\"current\":\"3\""),
                    "V=12, R=4 should yield I=3");
        }

        @Test
        void givenVoltageAndResistanceReturnsPower() {
            final String result = tool.ohmsLaw(
                    "12", EMPTY_PARAM, "4", EMPTY_PARAM);
            assertTrue(result.contains("\"power\":\"36\""),
                    "V=12, R=4 should yield P=36");
        }

        @Test
        void givenVoltageAndCurrentReturnsResistance() {
            final String result = tool.ohmsLaw(
                    "10", "2", EMPTY_PARAM, EMPTY_PARAM);
            assertTrue(result.contains("\"resistance\":\"5\""),
                    "V=10, I=2 should yield R=5");
        }

        @Test
        void givenVoltageAndCurrentReturnsPower() {
            final String result = tool.ohmsLaw(
                    "10", "2", EMPTY_PARAM, EMPTY_PARAM);
            assertTrue(result.contains("\"power\":\"20\""),
                    "V=10, I=2 should yield P=20");
        }

        @Test
        void onlyOneKnownReturnsError() {
            final String result = tool.ohmsLaw(
                    "12", EMPTY_PARAM, EMPTY_PARAM, EMPTY_PARAM);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Only 1 known param should return error");
        }

        @Test
        void threeKnownsReturnsError() {
            final String result = tool.ohmsLaw(
                    "12", "3", "4", EMPTY_PARAM);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "3 known params should return error");
        }
    }

    @Nested
    @DisplayName("resistorCombination")
    class ResistorCombinationTests {

        @Test
        void seriesCombinationSumsValues() {
            final String result = tool.resistorCombination(
                    "100,200,300", SERIES);
            assertClose(new BigDecimal("600"), result,
                    "Series [100,200,300] should be 600");
        }

        @Test
        void parallelCombinationComputesReciprocal() {
            final String result = tool.resistorCombination(
                    "100,100", PARALLEL);
            assertClose(new BigDecimal("50"), result,
                    "Parallel [100,100] should be 50");
        }
    }

    @Nested
    @DisplayName("capacitorCombination")
    class CapacitorCombinationTests {

        @Test
        void seriesCapacitorsUseReciprocalSum() {
            final String result = tool.capacitorCombination(
                    "0.001,0.001", SERIES);
            assertClose(new BigDecimal("0.0005"), result,
                    "Series [0.001,0.001] should be 0.0005");
        }

        @Test
        void parallelCapacitorsSumValues() {
            final String result = tool.capacitorCombination(
                    "0.001,0.002", PARALLEL);
            assertClose(new BigDecimal("0.003"), result,
                    "Parallel [0.001,0.002] should be 0.003");
        }
    }

    @Nested
    @DisplayName("inductorCombination")
    class InductorCombinationTests {

        @Test
        void seriesInductorsSumValues() {
            final String result = tool.inductorCombination(
                    "0.1,0.2,0.3", SERIES);
            assertClose(new BigDecimal("0.6"), result,
                    "Series [0.1,0.2,0.3] should be 0.6");
        }

        @Test
        void parallelInductorsUseReciprocalSum() {
            final String result = tool.inductorCombination(
                    "0.1,0.1", PARALLEL);
            assertClose(new BigDecimal("0.05"), result,
                    "Parallel [0.1,0.1] should be 0.05");
        }
    }

    @Nested
    @DisplayName("voltageDivider")
    class VoltageDividerTests {

        @Test
        void standardVoltageDivider() {
            final String result = tool.voltageDivider(
                    "12", VALUE_1000, VALUE_2000);
            assertClose(new BigDecimal("8"), result,
                    "Vin=12, R1=1000, R2=2000 should yield Vout=8");
        }
    }

    @Nested
    @DisplayName("currentDivider")
    class CurrentDividerTests {

        @Test
        void currentDividerBranch1() {
            final String result = tool.currentDivider(
                    "3", VALUE_100, VALUE_200);
            assertTrue(result.contains("\"i1\""),
                    "Result should contain i1 branch current");
        }

        @Test
        void currentDividerBranch2() {
            final String result = tool.currentDivider(
                    "3", VALUE_100, VALUE_200);
            assertTrue(result.contains("\"i2\""),
                    "Result should contain i2 branch current");
        }

        @Test
        void currentDividerBranch1Value() {
            final String result = tool.currentDivider(
                    "3", VALUE_100, VALUE_200);
            assertClose(EXPECTED_TWO,
                    extractJsonValue(result, "i1"),
                    "I1 = It*R2/(R1+R2) = 3*200/300 = 2");
        }

        @Test
        void currentDividerBranch2Value() {
            final String result = tool.currentDivider(
                    "3", VALUE_100, VALUE_200);
            assertClose(BigDecimal.ONE,
                    extractJsonValue(result, "i2"),
                    "I2 = It*R1/(R1+R2) = 3*100/300 = 1");
        }
    }

    @Nested
    @DisplayName("rcTimeConstant")
    class RcTimeConstantTests {

        @Test
        void tauIsRTimesC() {
            final String result = tool.rcTimeConstant(
                    VALUE_1000, ONE_MICROFARAD);
            assertClose(EXPECTED_TAU,
                    extractJsonValue(result, JSON_KEY_TAU),
                    "R=1000, C=1e-6 should yield tau=0.001");
        }

        @Test
        void cutoffFrequencyApproximation() {
            final String result = tool.rcTimeConstant(
                    VALUE_1000, ONE_MICROFARAD);
            assertClose(EXPECTED_CUTOFF,
                    extractJsonValue(result, JSON_KEY_CUTOFF),
                    "fc = 1/(2*pi*R*C) should be ~159.15");
        }
    }

    @Nested
    @DisplayName("rlTimeConstant")
    class RlTimeConstantTests {

        @Test
        void tauIsLOverR() {
            final String result = tool.rlTimeConstant(
                    VALUE_100, "0.1");
            assertClose(EXPECTED_TAU,
                    extractJsonValue(result, JSON_KEY_TAU),
                    "R=100, L=0.1 should yield tau=0.001");
        }

        @Test
        void cutoffFrequencyRl() {
            final String result = tool.rlTimeConstant(
                    VALUE_100, "0.1");
            assertClose(EXPECTED_CUTOFF,
                    extractJsonValue(result, JSON_KEY_CUTOFF),
                    "fc = R/(2*pi*L) should be ~159.15");
        }
    }

    @Nested
    @DisplayName("rlcResonance")
    class RlcResonanceTests {

        @Test
        void resonantFrequencyApproximation() {
            final String result = tool.rlcResonance(
                    VALUE_100, INDUCTANCE_1MH, ONE_MICROFARAD);
            assertClose(new BigDecimal("5032.9"),
                    extractJsonValue(result, "resonantFrequency"),
                    "L=0.001, C=1e-6 should yield fr ~5032.9");
        }

        @Test
        void resultContainsQFactor() {
            final String result = tool.rlcResonance(
                    VALUE_100, INDUCTANCE_1MH, ONE_MICROFARAD);
            assertTrue(result.contains("\"qFactor\""),
                    "Result should contain Q factor");
        }

        @Test
        void resultContainsBandwidth() {
            final String result = tool.rlcResonance(
                    VALUE_100, INDUCTANCE_1MH, ONE_MICROFARAD);
            assertTrue(result.contains("\"bandwidth\""),
                    "Result should contain bandwidth");
        }
    }

    @Nested
    @DisplayName("impedance")
    class ImpedanceTests {

        @Test
        void resultContainsMagnitude() {
            final String result = tool.impedance(
                    VALUE_100, "0.01", "0.0001", VALUE_1000);
            assertTrue(result.contains("\"magnitude\""),
                    "Result should contain magnitude");
        }

        @Test
        void resultContainsPhase() {
            final String result = tool.impedance(
                    VALUE_100, "0.01", "0.0001", VALUE_1000);
            assertTrue(result.contains("\"phase\""),
                    "Result should contain phase");
        }
    }

    @Nested
    @DisplayName("decibelConvert")
    class DecibelConvertTests {

        @Test
        void powerToDbRatio2() {
            final String result = tool.decibelConvert(
                    "2", POWER_TO_DB);
            assertClose(new BigDecimal("3.0103"), result,
                    "Power ratio 2 should be ~3.0103 dB");
        }

        @Test
        void dbToPower3dB() {
            final String result = tool.decibelConvert(
                    "3", DB_TO_POWER);
            assertClose(EXPECTED_TWO, result,
                    "3 dB should map to power ratio ~2.0");
        }

        @Test
        void voltageToDbRatio2() {
            final String result = tool.decibelConvert(
                    "2", VOLTAGE_TO_DB);
            assertClose(new BigDecimal("6.0206"), result,
                    "Voltage ratio 2 should be ~6.0206 dB");
        }

        @Test
        void dbToVoltage6dB() {
            final String result = tool.decibelConvert(
                    "6", DB_TO_VOLTAGE);
            assertClose(EXPECTED_TWO, result,
                    "6 dB should map to voltage ratio ~2.0");
        }
    }

    @Nested
    @DisplayName("filterCutoff")
    class FilterCutoffTests {

        @Test
        void lowpassCutoffFrequency() {
            final String result = tool.filterCutoff(
                    VALUE_1000, ONE_MICROFARAD, LOWPASS);
            assertTrue(result.contains("\"cutoffFrequency\""),
                    "Result should contain cutoff frequency");
        }

        @Test
        void lowpassFilterType() {
            final String result = tool.filterCutoff(
                    VALUE_1000, ONE_MICROFARAD, LOWPASS);
            assertTrue(result.contains("\"filterType\":\"lowpass\""),
                    "Result should contain filter type lowpass");
        }

        @Test
        void lowpassCutoffValue() {
            final String result = tool.filterCutoff(
                    VALUE_1000, ONE_MICROFARAD, LOWPASS);
            assertClose(EXPECTED_CUTOFF,
                    extractJsonValue(result, JSON_KEY_CUTOFF),
                    "fc = 1/(2*pi*1000*1e-6) should be ~159.15");
        }
    }

    @Nested
    @DisplayName("ledResistor")
    class LedResistorTests {

        @Test
        void standardLedResistorValue() {
            final String result = tool.ledResistor(
                    "5", "2", "0.02");
            assertClose(new BigDecimal("150"), result,
                    "Vs=5, Vf=2, If=0.02 should yield R=150");
        }

        @Test
        void supplyBelowForwardVoltageReturnsError() {
            final String result = tool.ledResistor(
                    "2", "5", "0.02");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Vs < Vf should return error");
        }
    }

    @Nested
    @DisplayName("wheatstoneBridge")
    class WheatstoneBridgeTests {

        @Test
        void balanceResistorValue() {
            final String result = tool.wheatstoneBridge(
                    VALUE_100, VALUE_200, VALUE_300);
            assertClose(new BigDecimal("600"), result,
                    "R4 = R3*R2/R1 = 300*200/100 = 600");
        }
    }

    private String extractJsonValue(final String json, final String key) {
        final String searchKey = "\"" + key + "\":\"";
        final int start = json.indexOf(searchKey) + searchKey.length();
        final int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
