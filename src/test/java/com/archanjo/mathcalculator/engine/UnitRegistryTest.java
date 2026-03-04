package com.archanjo.mathcalculator.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.MathContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnitRegistryTest {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal TOLERANCE =
            new BigDecimal("0.0000000001");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final String BPS_UNIT = "bps";

    private void assertCloseEnough(
            final BigDecimal expected, final BigDecimal actual,
            final String msg) {
        assertEquals(true,
                expected.subtract(actual, PRECISION)
                        .abs().compareTo(TOLERANCE) < 0,
                msg + " — expected: " + expected
                        + ", actual: " + actual);
    }

    @Nested
    @DisplayName("DATA_STORAGE")
    class DataStorageTests {

        @Test
        void bytesToKilobytes() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("1024"), "byte", "kb");
            assertCloseEnough(BigDecimal.ONE, result,
                    "1024 bytes = 1 KB");
        }

        @Test
        void megabytesToBits() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "mb", "bit");
            assertCloseEnough(new BigDecimal("8388608"),
                    result, "1 MB = 8388608 bits");
        }
    }

    @Nested
    @DisplayName("LENGTH")
    class LengthTests {

        @Test
        void kilometerToMile() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "km", "mi");
            assertCloseEnough(new BigDecimal("0.6213711922"),
                    result, "1 km = 0.621371 mi");
        }

        @Test
        void inchToCentimeter() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "in", "cm");
            assertCloseEnough(new BigDecimal("2.54"),
                    result, "1 in = 2.54 cm (exact SI)");
        }

        @Test
        void roundTripKmMi() {
            final BigDecimal toMi = UnitRegistry.convert(
                    BigDecimal.ONE, "km", "mi");
            final BigDecimal back = UnitRegistry.convert(
                    toMi, "mi", "km");
            assertCloseEnough(BigDecimal.ONE, back,
                    "km -> mi -> km round-trip");
        }
    }

    @Nested
    @DisplayName("MASS")
    class MassTests {

        @Test
        void poundToKilogram() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "lb", "kg");
            assertCloseEnough(new BigDecimal("0.45359237"),
                    result, "1 lb = 0.45359237 kg (exact)");
        }

        @Test
        void ounceToGram() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "oz", "g");
            assertCloseEnough(new BigDecimal("28.349523125"),
                    result, "1 oz = 28.349523125 g");
        }
    }

    @Nested
    @DisplayName("VOLUME")
    class VolumeTests {

        @Test
        void gallonToLiter() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "usgal", "l");
            assertCloseEnough(new BigDecimal("3.785411784"),
                    result, "1 US gal = 3.785411784 L");
        }

        @Test
        void cupToTablespoon() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "uscup", "tbsp");
            assertCloseEnough(new BigDecimal("16"),
                    result, "1 US cup = 16 tbsp");
        }
    }

    @Nested
    @DisplayName("TEMPERATURE")
    class TemperatureTests {

        @Test
        void celsiusToFahrenheit() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("100"), "c", "f");
            assertCloseEnough(new BigDecimal("212"),
                    result, "100 C = 212 F");
        }

        @Test
        void fahrenheitToCelsius() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("32"), "f", "c");
            assertCloseEnough(BigDecimal.ZERO, result,
                    "32 F = 0 C");
        }

        @Test
        void celsiusToKelvin() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ZERO, "c", "k");
            assertCloseEnough(new BigDecimal("273.15"),
                    result, "0 C = 273.15 K");
        }

        @Test
        void kelvinToCelsius() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("373.15"), "k", "c");
            assertCloseEnough(new BigDecimal("100"),
                    result, "373.15 K = 100 C");
        }

        @Test
        void sameUnitReturnsInput() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("25"), "c", "c");
            assertCloseEnough(new BigDecimal("25"),
                    result, "C -> C should return same value");
        }
    }

    @Nested
    @DisplayName("TIME")
    class TimeTests {

        @Test
        void hourToSeconds() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "h", "s");
            assertCloseEnough(new BigDecimal("3600"),
                    result, "1 h = 3600 s");
        }

        @Test
        void dayToHour() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "d", "h");
            assertCloseEnough(new BigDecimal("24"),
                    result, "1 day = 24 hours");
        }
    }

    @Nested
    @DisplayName("SPEED")
    class SpeedTests {

        @Test
        void kmhToMs() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("3.6"), "km/h", "m/s");
            assertCloseEnough(BigDecimal.ONE, result,
                    "3.6 km/h = 1 m/s");
        }

        @Test
        void mphToKmh() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "mph", "km/h");
            assertCloseEnough(new BigDecimal("1.609344"),
                    result, "1 mph = 1.609344 km/h");
        }
    }

    @Nested
    @DisplayName("AREA")
    class AreaTests {

        @Test
        void hectareToM2() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "ha", "m2");
            assertCloseEnough(new BigDecimal("10000"),
                    result, "1 ha = 10000 m2");
        }
    }

    @Nested
    @DisplayName("ENERGY")
    class EnergyTests {

        @Test
        void kilocalorieToJoule() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "kcal", "j");
            assertCloseEnough(new BigDecimal("4184"),
                    result, "1 kcal = 4184 J");
        }
    }

    @Nested
    @DisplayName("FORCE")
    class ForceTests {

        @Test
        void lbfToNewton() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "lbf", "n");
            assertCloseEnough(new BigDecimal("4.4482216153"),
                    result, "1 lbf = 4.44822 N");
        }
    }

    @Nested
    @DisplayName("PRESSURE")
    class PressureTests {

        @Test
        void atmToPascal() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "atm", "pa");
            assertCloseEnough(new BigDecimal("101325"),
                    result, "1 atm = 101325 Pa");
        }

        @Test
        void psiRoundTrip() {
            final BigDecimal toPa = UnitRegistry.convert(
                    BigDecimal.ONE, "psi", "pa");
            final BigDecimal back = UnitRegistry.convert(
                    toPa, "pa", "psi");
            assertCloseEnough(BigDecimal.ONE, back,
                    "psi -> pa -> psi round-trip");
        }
    }

    @Nested
    @DisplayName("POWER")
    class PowerTests {

        @Test
        void kilowattToWatt() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "kw", "w");
            assertCloseEnough(THOUSAND,
                    result, "1 kW = 1000 W");
        }
    }

    @Nested
    @DisplayName("DENSITY")
    class DensityTests {

        @Test
        void gCm3ToKgM3() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "g/cm3", "kg/m3");
            assertCloseEnough(THOUSAND,
                    result, "1 g/cm3 = 1000 kg/m3");
        }
    }

    @Nested
    @DisplayName("FREQUENCY")
    class FrequencyTests {

        @Test
        void ghzToHz() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "ghz", "hz");
            assertCloseEnough(new BigDecimal("1000000000"),
                    result, "1 GHz = 1e9 Hz");
        }

        @Test
        void rpmToHz() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("60"), "rpm", "hz");
            assertCloseEnough(BigDecimal.ONE, result,
                    "60 RPM = 1 Hz");
        }
    }

    @Nested
    @DisplayName("ANGLE")
    class AngleTests {

        @Test
        void degreeToRadian() {
            final BigDecimal result = UnitRegistry.convert(
                    new BigDecimal("180"), "deg", "rad");
            assertCloseEnough(new BigDecimal("3.1415926536"),
                    result, "180 deg = pi rad");
        }

        @Test
        void turnToDegree() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "turn", "deg");
            assertCloseEnough(new BigDecimal("360"),
                    result, "1 turn = 360 deg");
        }
    }

    @Nested
    @DisplayName("Gas mark")
    class GasMarkTests {

        @Test
        void gasMark4ToCelsius() {
            assertEquals(0,
                    UnitRegistry.gasMarkToCelsius(4)
                            .compareTo(new BigDecimal("180")),
                    "Gas mark 4 = 180 C");
        }

        @Test
        void celsiusToGasMark() {
            assertEquals(6,
                    UnitRegistry.celsiusToGasMark(
                            new BigDecimal("200")),
                    "200 C = gas mark 6");
        }
    }

    @Nested
    @DisplayName("DATA_RATE")
    class DataRateTests {

        @Test
        void kbpsToBps() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "kbps", BPS_UNIT);
            assertCloseEnough(THOUSAND,
                    result, "1 kbps = 1000 bps");
        }

        @Test
        void bypsToBps() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "byps", BPS_UNIT);
            assertCloseEnough(new BigDecimal("8"),
                    result, "1 byps = 8 bps");
        }

        @Test
        void roundTripGbpsBpsGbps() {
            final BigDecimal toBps = UnitRegistry.convert(
                    BigDecimal.ONE, "gbps", BPS_UNIT);
            final BigDecimal back = UnitRegistry.convert(
                    toBps, BPS_UNIT, "gbps");
            assertCloseEnough(BigDecimal.ONE, back,
                    "gbps -> bps -> gbps round-trip");
        }
    }

    @Nested
    @DisplayName("RESISTANCE")
    class ResistanceTests {

        @Test
        void kohmToOhm() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "kohm", "ohm");
            assertCloseEnough(THOUSAND,
                    result, "1 kohm = 1000 ohm");
        }

        @Test
        void roundTripMohmOhmMohm() {
            final BigDecimal toOhm = UnitRegistry.convert(
                    BigDecimal.ONE, "mohm", "ohm");
            final BigDecimal back = UnitRegistry.convert(
                    toOhm, "ohm", "mohm");
            assertCloseEnough(BigDecimal.ONE, back,
                    "mohm -> ohm -> mohm round-trip");
        }
    }

    @Nested
    @DisplayName("CAPACITANCE")
    class CapacitanceTests {

        @Test
        void ufToPf() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "uf", "pf");
            assertCloseEnough(new BigDecimal("1000000"),
                    result, "1 uf = 1000000 pf");
        }

        @Test
        void roundTripNfFdNf() {
            final BigDecimal toFd = UnitRegistry.convert(
                    BigDecimal.ONE, "nf", "fd");
            final BigDecimal back = UnitRegistry.convert(
                    toFd, "fd", "nf");
            assertCloseEnough(BigDecimal.ONE, back,
                    "nf -> fd -> nf round-trip");
        }
    }

    @Nested
    @DisplayName("INDUCTANCE")
    class InductanceTests {

        @Test
        void mhyToHy() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "mhy", "hy");
            assertCloseEnough(new BigDecimal("0.001"),
                    result, "1 mhy = 0.001 hy");
        }

        @Test
        void uhyToHy() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "uhy", "hy");
            assertCloseEnough(new BigDecimal("0.000001"),
                    result, "1 uhy = 0.000001 hy");
        }
    }

    @Nested
    @DisplayName("VOLTAGE")
    class VoltageTests {

        @Test
        void kvltToVlt() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "kvlt", "vlt");
            assertCloseEnough(THOUSAND,
                    result, "1 kvlt = 1000 vlt");
        }

        @Test
        void mvltToVlt() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "mvlt", "vlt");
            assertCloseEnough(new BigDecimal("0.001"),
                    result, "1 mvlt = 0.001 vlt");
        }
    }

    @Nested
    @DisplayName("CURRENT")
    class CurrentTests {

        @Test
        void mampToAmp() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "mamp", "amp");
            assertCloseEnough(new BigDecimal("0.001"),
                    result, "1 mamp = 0.001 amp");
        }

        @Test
        void uampToAmp() {
            final BigDecimal result = UnitRegistry.convert(
                    BigDecimal.ONE, "uamp", "amp");
            assertCloseEnough(new BigDecimal("0.000001"),
                    result, "1 uamp = 0.000001 amp");
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorTests {

        @Test
        void unknownUnitThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> UnitRegistry.convert(
                            BigDecimal.ONE, "xxx", "km"),
                    "Unknown unit should throw");
        }

        @Test
        void crossCategoryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> UnitRegistry.convert(
                            BigDecimal.ONE, "km", "kg"),
                    "Cross-category should throw");
        }

        @Test
        void temperatureFactorThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> UnitRegistry.getConversionFactor("c", "f"),
                    "Temperature factor should throw");
        }
    }

    @Nested
    @DisplayName("Reference methods")
    class ReferenceTests {

        @Test
        void listCategoriesReturns21() {
            assertEquals(21,
                    UnitRegistry.listCategories().size(),
                    "Should have 21 categories");
        }

        @Test
        void listLengthUnitsNotEmpty() {
            assertFalse(
                    UnitRegistry.listUnits(UnitCategory.LENGTH)
                            .isEmpty(),
                    "LENGTH should have units");
        }

        @Test
        void findKnownUnit() {
            assertNotNull(UnitRegistry.findUnit("km"),
                    "km should be found");
        }

        @Test
        void explainLinearConversion() {
            assertEquals(true,
                    UnitRegistry.explainConversion("km", "mi")
                            .contains("kilometer"),
                    "Should mention unit names");
        }
    }
}
