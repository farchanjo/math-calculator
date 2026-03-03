package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FinancialCalculatorToolTest {

    private static final double TOLERANCE = 0.01;
    private static final String TWELVE_THOUSAND = "12000";

    private final FinancialCalculatorTool tool = new FinancialCalculatorTool();

    @Nested
    @DisplayName("compoundInterest")
    class CompoundInterest {

        @Test
        void standardCase() {
            final String result = tool.compoundInterest("1000", "5", "10", 12);
            final double actual = new BigDecimal(result).doubleValue();
            assertEquals(1_647.01, actual, TOLERANCE,
                    "P=1000, r=5%, t=10y, n=12 should yield ~1647.01");
        }

        @Test
        void zeroPrincipalThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.compoundInterest("0", "5", "10", 12),
                    "Zero principal should throw IllegalArgumentException");
        }

        @Test
        void negativePrincipalThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.compoundInterest("-500", "5", "10", 12),
                    "Negative principal should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("loanPayment")
    class LoanPayment {

        @Test
        void standardMortgage() {
            final String result = tool.loanPayment("200000", "6", "30");
            final double actual = new BigDecimal(result).doubleValue();
            assertEquals(1_199.10, actual, TOLERANCE,
                    "200k loan at 6% for 30y should yield ~1199.10");
        }

        @Test
        void zeroRate() {
            final String result = tool.loanPayment(TWELVE_THOUSAND, "0", "1");
            final double actual = new BigDecimal(result).doubleValue();
            assertEquals(1_000.0, actual, TOLERANCE,
                    "12000 at 0% for 1y should yield 1000/month");
        }
    }

    @Nested
    @DisplayName("presentValue")
    class PresentValue {

        @Test
        void standardCase() {
            final String result = tool.presentValue("10000", "5", "10");
            final double actual = new BigDecimal(result).doubleValue();
            assertEquals(6_139.13, actual, TOLERANCE,
                    "FV=10000 at 5% over 10y should yield PV ~6139.13");
        }
    }

    @Nested
    @DisplayName("futureValueAnnuity")
    class FutureValueAnnuity {

        @Test
        void standardCase() {
            final String result = tool.futureValueAnnuity("1000", "5", "10");
            final double actual = new BigDecimal(result).doubleValue();
            assertEquals(12_577.89, actual, TOLERANCE,
                    "PMT=1000 at 5% for 10y should yield FV ~12577.89");
        }

        @Test
        void zeroRate() {
            final String result = tool.futureValueAnnuity("1000", "0", "10");
            final double actual = new BigDecimal(result).doubleValue();
            assertEquals(10_000.0, actual, TOLERANCE,
                    "PMT=1000 at 0% for 10y should yield 10000");
        }
    }

    @Nested
    @DisplayName("returnOnInvestment")
    class ReturnOnInvest {

        @Test
        void fiftyPercent() {
            final String result = tool.returnOnInvestment("15000", "10000");
            final BigDecimal roi = new BigDecimal(result);
            assertEquals(0, roi.compareTo(new BigDecimal("50")),
                    "gain=15000, cost=10000 should yield ROI of 50%");
        }

        @Test
        void zeroCostThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.returnOnInvestment("5000", "0"),
                    "Zero cost should throw IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("amortizationSchedule")
    class Amortization {

        @Test
        void startsWithJsonArray() {
            final String result = tool.amortizationSchedule(TWELVE_THOUSAND, "12", "1");
            assertTrue(result.startsWith("["),
                    "Amortization schedule should be a JSON array starting with '['");
        }

        @Test
        void containsMonthField() {
            final String result = tool.amortizationSchedule(TWELVE_THOUSAND, "12", "1");
            assertTrue(result.contains("\"month\""),
                    "Each entry should contain a 'month' field");
        }

        @Test
        void hasTwelveEntries() {
            final String result = tool.amortizationSchedule(TWELVE_THOUSAND, "12", "1");
            final long entryCount = result.chars()
                    .mapToObj(idx -> (char) idx)
                    .filter(chr -> chr == '{')
                    .count();
            assertEquals(12, entryCount,
                    "A 1-year schedule should contain exactly 12 monthly entries");
        }
    }
}
