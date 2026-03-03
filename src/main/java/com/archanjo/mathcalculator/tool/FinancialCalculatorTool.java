package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FinancialCalculatorTool {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int INTERNAL_SCALE = 20;
    private static final int DISPLAY_SCALE = 2;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int SINGLE_MONTH = 1;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String YEARS_LABEL = "Years";

    @Tool(description = "Compute compound interest. Returns final amount.")
    public String compoundInterest(
            @ToolParam(description = "Principal amount")
            final String principal,
            @ToolParam(description = "Annual rate as % (e.g. '5' for 5%)")
            final String annualRate,
            @ToolParam(description = "Duration in years")
            final String years,
            @ToolParam(description = "Compounds per year")
            final int compoundsPerYear) {
        final BigDecimal principalAmt = new BigDecimal(principal);
        final BigDecimal rate = new BigDecimal(annualRate);
        final BigDecimal yearsDecimal = new BigDecimal(years);

        validatePositive(principalAmt, "Principal");
        validateNonNegative(rate, "Annual rate");
        validatePositive(yearsDecimal, YEARS_LABEL);
        if (compoundsPerYear <= 0) {
            throw new IllegalArgumentException(
                    "Compounds per year must be greater than zero");
        }
        final BigDecimal compoundsCount =
                BigDecimal.valueOf(compoundsPerYear);

        final BigDecimal annualRateDec =
                rate.divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP);
        final BigDecimal rateOverComp = annualRateDec.divide(
                compoundsCount, INTERNAL_SCALE, RoundingMode.HALF_UP);
        final BigDecimal onePlusRate =
                BigDecimal.ONE.add(rateOverComp, PRECISION);
        final int totalCompounds =
                compoundsCount.multiply(yearsDecimal, PRECISION)
                        .intValueExact();

        final BigDecimal result = principalAmt.multiply(
                onePlusRate.pow(totalCompounds, PRECISION), PRECISION);
        return result.stripTrailingZeros().toPlainString();
    }

    @Tool(description = "Compute fixed monthly loan payment.")
    public String loanPayment(
            @ToolParam(description = "Loan principal")
            final String principal,
            @ToolParam(description = "Annual rate as % (e.g. '6' for 6%)")
            final String annualRate,
            @ToolParam(description = "Duration in years")
            final String years) {
        final BigDecimal principalAmt = new BigDecimal(principal);
        final BigDecimal rate = new BigDecimal(annualRate);
        final BigDecimal yearsDecimal = new BigDecimal(years);

        validatePositive(principalAmt, "Principal");
        validatePositive(yearsDecimal, YEARS_LABEL);

        final int totalMonths = yearsDecimal
                .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR), PRECISION)
                .intValueExact();

        final String result;
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            result = principalAmt
                    .divide(BigDecimal.valueOf(totalMonths),
                            INTERNAL_SCALE, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString();
        } else {
            final BigDecimal monthlyRate = rate
                    .divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(MONTHS_PER_YEAR),
                            INTERNAL_SCALE, RoundingMode.HALF_UP);

            final BigDecimal onePlusR =
                    BigDecimal.ONE.add(monthlyRate, PRECISION);
            final BigDecimal onePlusRPowN =
                    onePlusR.pow(totalMonths, PRECISION);

            final BigDecimal numerator = principalAmt
                    .multiply(monthlyRate, PRECISION)
                    .multiply(onePlusRPowN, PRECISION);
            final BigDecimal denominator =
                    onePlusRPowN.subtract(BigDecimal.ONE, PRECISION);

            final BigDecimal payment = numerator.divide(
                    denominator, INTERNAL_SCALE, RoundingMode.HALF_UP);
            result = payment.stripTrailingZeros().toPlainString();
        }
        return result;
    }

    @Tool(description = "Compute present value of a future amount.")
    public String presentValue(
            @ToolParam(description = "Future value amount")
            final String futureValue,
            @ToolParam(description = "Annual discount rate as % (e.g. '8')")
            final String annualRate,
            @ToolParam(description = "Time horizon in years")
            final String years) {
        final BigDecimal futureVal = new BigDecimal(futureValue);
        final BigDecimal rate = new BigDecimal(annualRate);
        final BigDecimal yearsDecimal = new BigDecimal(years);

        validatePositive(futureVal, "Future value");
        validatePositive(yearsDecimal, YEARS_LABEL);

        final BigDecimal annualRateDec =
                rate.divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP);
        final BigDecimal onePlusR =
                BigDecimal.ONE.add(annualRateDec, PRECISION);
        final int exponent = yearsDecimal.intValueExact();
        final BigDecimal divisor = onePlusR.pow(exponent, PRECISION);

        final BigDecimal presentVal = futureVal.divide(
                divisor, INTERNAL_SCALE, RoundingMode.HALF_UP);
        return presentVal.stripTrailingZeros().toPlainString();
    }

    @Tool(description = "Compute future value of an ordinary annuity.")
    public String futureValueAnnuity(
            @ToolParam(description = "Periodic payment amount")
            final String payment,
            @ToolParam(description = "Annual rate as % (e.g. '7' for 7%)")
            final String annualRate,
            @ToolParam(description = "Investment period in years")
            final String years) {
        final BigDecimal pmt = new BigDecimal(payment);
        final BigDecimal rate = new BigDecimal(annualRate);
        final BigDecimal yearsDecimal = new BigDecimal(years);

        validatePositive(pmt, "Payment");
        validatePositive(yearsDecimal, YEARS_LABEL);

        final String result;
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            result = pmt.multiply(yearsDecimal, PRECISION)
                    .stripTrailingZeros().toPlainString();
        } else {
            final BigDecimal annualRateDec = rate.divide(
                    HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP);
            final int exponent = yearsDecimal.intValueExact();
            final BigDecimal onePlusRPowN = BigDecimal.ONE
                    .add(annualRateDec, PRECISION)
                    .pow(exponent, PRECISION);
            final BigDecimal numerator =
                    onePlusRPowN.subtract(BigDecimal.ONE, PRECISION);

            final BigDecimal futureVal = pmt
                    .multiply(numerator, PRECISION)
                    .divide(annualRateDec, INTERNAL_SCALE,
                            RoundingMode.HALF_UP);
            result = futureVal.stripTrailingZeros().toPlainString();
        }
        return result;
    }

    @Tool(description = "Compute ROI as a percentage.")
    public String returnOnInvestment(
            @ToolParam(description = "Total gain (revenue)")
            final String gain,
            @ToolParam(description = "Total cost (non-zero)")
            final String cost) {
        final BigDecimal gainAmount = new BigDecimal(gain);
        final BigDecimal costAmount = new BigDecimal(cost);

        if (costAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                    "Cost must not be zero");
        }

        final BigDecimal roi = gainAmount.subtract(costAmount, PRECISION)
                .divide(costAmount, INTERNAL_SCALE,
                        RoundingMode.HALF_UP)
                .multiply(HUNDRED, PRECISION);
        return roi.stripTrailingZeros().toPlainString();
    }

    @Tool(description = "Generate monthly amortization schedule as JSON.")
    public String amortizationSchedule(
            @ToolParam(description = "Loan principal")
            final String principal,
            @ToolParam(description = "Annual rate as % (e.g. '5' for 5%)")
            final String annualRate,
            @ToolParam(description = "Repayment term in years")
            final String years) {
        final BigDecimal principalAmt = new BigDecimal(principal);
        final BigDecimal rate = new BigDecimal(annualRate);
        final BigDecimal yearsDecimal = new BigDecimal(years);

        validatePositive(principalAmt, "Principal");
        validatePositive(yearsDecimal, YEARS_LABEL);

        final int totalMonths = yearsDecimal
                .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR), PRECISION)
                .intValueExact();

        final BigDecimal monthlyPayment;
        final BigDecimal monthlyRate;

        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyRate = BigDecimal.ZERO;
            monthlyPayment = principalAmt.divide(
                    BigDecimal.valueOf(totalMonths),
                    INTERNAL_SCALE, RoundingMode.HALF_UP);
        } else {
            monthlyRate = rate
                    .divide(HUNDRED, INTERNAL_SCALE,
                            RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(MONTHS_PER_YEAR),
                            INTERNAL_SCALE, RoundingMode.HALF_UP);

            final BigDecimal onePlusR =
                    BigDecimal.ONE.add(monthlyRate, PRECISION);
            final BigDecimal onePlusRPowN =
                    onePlusR.pow(totalMonths, PRECISION);
            final BigDecimal numerator = principalAmt
                    .multiply(monthlyRate, PRECISION)
                    .multiply(onePlusRPowN, PRECISION);
            final BigDecimal denominator =
                    onePlusRPowN.subtract(BigDecimal.ONE, PRECISION);
            monthlyPayment = numerator.divide(
                    denominator, INTERNAL_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal balance = principalAmt;
        final StringBuilder builder = new StringBuilder(1024);
        builder.append('[');

        for (int month = 1; month <= totalMonths; month++) {
            final BigDecimal interest = balance
                    .multiply(monthlyRate, PRECISION)
                    .setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

            final BigDecimal pmtAmount;
            final BigDecimal principalPart;

            if (month == totalMonths) {
                principalPart = balance;
                pmtAmount = principalPart.add(interest, PRECISION);
                balance = BigDecimal.ZERO;
            } else {
                pmtAmount = monthlyPayment;
                principalPart = pmtAmount.subtract(interest, PRECISION);
                balance = balance.subtract(principalPart, PRECISION);
            }

            if (month > SINGLE_MONTH) {
                builder.append(',');
            }

            builder.append("{\"month\":").append(month)
                    .append(",\"payment\":\"")
                    .append(formatCurrency(pmtAmount))
                    .append("\",\"principal\":\"")
                    .append(formatCurrency(principalPart))
                    .append("\",\"interest\":\"")
                    .append(formatCurrency(interest))
                    .append("\",\"balance\":\"")
                    .append(formatCurrency(balance))
                    .append("\"}");
        }

        builder.append(']');
        return builder.toString();
    }

    private String formatCurrency(final BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private void validatePositive(final BigDecimal value,
                                  final String name) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    name + " must be greater than zero");
        }
    }

    private void validateNonNegative(final BigDecimal value,
                                     final String name) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    name + " must not be negative");
        }
    }
}
