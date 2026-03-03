package com.archanjo.mathcalculator.tool;

import java.util.StringJoiner;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"preview", "incubating"})
public class VectorCalculatorTool {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final String EMPTY_ARRAY_MSG = "Input array must not be empty";
    @Tool(description = "Sum all elements of a numeric array.")
    public String sumArray(
            @ToolParam(description = "Comma-separated numbers, e.g. '1.0,2.5,3.0'")
            final String numbers) {
        final double[] array = parseArray(numbers);
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MSG);
        }

        DoubleVector acc = DoubleVector.zero(SPECIES);
        int idx = 0;
        for (; idx < SPECIES.loopBound(array.length); idx += SPECIES.length()) {
            final DoubleVector vectorA = DoubleVector.fromArray(SPECIES, array, idx);
            acc = acc.add(vectorA);
        }
        double result = acc.reduceLanes(VectorOperators.ADD);
        for (; idx < array.length; idx++) {
            result += array[idx];
        }

        return String.valueOf(result);
    }

    @Tool(description = "Compute dot product of two numeric arrays.")
    public String dotProduct(
            @ToolParam(description = "First comma-separated numbers")
            final String first,
            @ToolParam(description = "Second comma-separated numbers (same length)")
            final String second) {
        final double[] arrayA = parseArray(first);
        final double[] arrayB = parseArray(second);
        if (arrayA.length == 0 || arrayB.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MSG);
        }
        if (arrayA.length != arrayB.length) {
            throw new IllegalArgumentException("Arrays must have equal length");
        }

        DoubleVector acc = DoubleVector.zero(SPECIES);
        int idx = 0;
        for (; idx < SPECIES.loopBound(arrayA.length); idx += SPECIES.length()) {
            final DoubleVector vectorA = DoubleVector.fromArray(SPECIES, arrayA, idx);
            final DoubleVector vectorB = DoubleVector.fromArray(SPECIES, arrayB, idx);
            acc = acc.add(vectorA.mul(vectorB));
        }
        double result = acc.reduceLanes(VectorOperators.ADD);
        for (; idx < arrayA.length; idx++) {
            result += arrayA[idx] * arrayB[idx];
        }

        return String.valueOf(result);
    }

    @Tool(description = "Multiply all array elements by a scalar.")
    public String scaleArray(
            @ToolParam(description = "Comma-separated numbers")
            final String numbers,
            @ToolParam(description = "Scalar multiplier")
            final String scalar) {
        final double[] array = parseArray(numbers);
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MSG);
        }
        final double factor = Double.parseDouble(scalar.trim());

        final double[] result = new double[array.length];
        final DoubleVector vScalar = DoubleVector.broadcast(SPECIES, factor);
        int idx = 0;
        for (; idx < SPECIES.loopBound(array.length); idx += SPECIES.length()) {
            final DoubleVector vectorA = DoubleVector.fromArray(SPECIES, array, idx);
            vectorA.mul(vScalar).intoArray(result, idx);
        }
        for (; idx < array.length; idx++) {
            result[idx] = array[idx] * factor;
        }

        final StringJoiner joiner = new StringJoiner(",");
        for (final double val : result) {
            joiner.add(String.valueOf(val));
        }
        return joiner.toString();
    }

    @Tool(description = "Compute Euclidean norm (magnitude) of a vector.")
    public String magnitudeArray(
            @ToolParam(description = "Comma-separated numbers")
            final String numbers) {
        final double[] array = parseArray(numbers);
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MSG);
        }

        DoubleVector acc = DoubleVector.zero(SPECIES);
        int idx = 0;
        for (; idx < SPECIES.loopBound(array.length); idx += SPECIES.length()) {
            final DoubleVector vectorA = DoubleVector.fromArray(SPECIES, array, idx);
            acc = acc.add(vectorA.mul(vectorA));
        }
        double sumOfSquares = acc.reduceLanes(VectorOperators.ADD);
        for (; idx < array.length; idx++) {
            sumOfSquares += array[idx] * array[idx];
        }

        return String.valueOf(StrictMath.sqrt(sumOfSquares));
    }

    private static double[] parseArray(final String input) {
        final String[] parts = input.split(",");
        final double[] result = new double[parts.length];
        for (int idx = 0; idx < parts.length; idx++) {
            result[idx] = Double.parseDouble(parts[idx].trim());
        }
        return result;
    }
}
