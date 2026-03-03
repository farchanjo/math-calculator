package com.archanjo.mathcalculator.engine;

import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * Pure Java recursive descent expression parser and evaluator.
 *
 * <p>Supports arithmetic operators ({@code +}, {@code -}, {@code *}, {@code /},
 * {@code %}, {@code ^}), unary negation, parentheses, decimal and scientific
 * notation numbers, named variables, and the built-in functions
 * {@code sin}, {@code cos}, {@code tan}, {@code log}, {@code log10},
 * {@code sqrt}, {@code abs}, {@code ceil}, and {@code floor}.
 *
 * <p>Trigonometric functions accept <strong>degrees</strong> and convert to
 * radians internally. All math is delegated to {@link StrictMath}.
 *
 * <h3>Operator precedence (lowest to highest)</h3>
 * <ol>
 *   <li>{@code +}, {@code -} (additive)</li>
 *   <li>{@code *}, {@code /}, {@code %} (multiplicative)</li>
 *   <li>{@code ^} (power, right-associative)</li>
 *   <li>Unary {@code -}</li>
 * </ol>
 *
 * <h3>Grammar</h3>
 * <pre>
 * expression = term (('+' | '-') term)*
 * term       = power (('*' | '/' | '%') power)*
 * power      = unary ('^' power)?          // right-associative via recursion
 * unary      = '-' unary | primary
 * primary    = NUMBER | VARIABLE | FUNCTION '(' expression ')' | '(' expression ')'
 * </pre>
 */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
        // static utility - no instantiation
    }

    /**
     * Evaluates a mathematical expression without variables.
     *
     * @param expression the expression to evaluate
     * @return the computed result
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static double evaluate(final String expression) {
        return evaluate(expression, Map.of());
    }

    /**
     * Evaluates a mathematical expression with variable substitution.
     *
     * @param expression the expression to evaluate
     * @param variables  a map of variable names to their values
     * @return the computed result
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static double evaluate(
            final String expression,
            final Map<String, Double> variables) {

        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException(
                    "Expression must not be null or blank");
        }
        final Parser parser = new Parser(expression, variables);
        final double result = parser.parseExpression();
        if (parser.pos < parser.input.length()) {
            throw new IllegalArgumentException(
                    "Unexpected character at position " + parser.pos
                            + ": '" + parser.input.charAt(
                                    parser.pos) + "'");
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Recursive descent parser
    // ------------------------------------------------------------------ //

    private static final class Parser {

        private static final char PLUS = '+';
        private static final char MINUS = '-';
        private static final char MULTIPLY = '*';
        private static final char DIVIDE = '/';
        private static final char MODULO = '%';
        private static final char POWER = '^';
        private static final char OPEN_PAREN = '(';
        private static final char CLOSE_PAREN = ')';
        private static final char DOT = '.';
        private static final char UNDERSCORE = '_';
        private static final String EXP_CHARS = "eE";
        private static final String SIGN_CHARS = "+-";

        private static final Map<String, DoubleUnaryOperator> FUNCTIONS =
                Map.of(
                    "sin", arg -> StrictMath.sin(
                            StrictMath.toRadians(arg)),
                    "cos", arg -> StrictMath.cos(
                            StrictMath.toRadians(arg)),
                    "tan", arg -> StrictMath.tan(
                            StrictMath.toRadians(arg)),
                    "log", StrictMath::log,
                    "log10", StrictMath::log10,
                    "sqrt", StrictMath::sqrt,
                    "abs", StrictMath::abs,
                    "ceil", StrictMath::ceil,
                    "floor", StrictMath::floor
                );

        final String input;
        private final Map<String, Double> variables;
        int pos;

        Parser(
                final String input,
                final Map<String, Double> variables) {

            this.input = input.replaceAll("\\s+", "");
            this.variables = variables;
            this.pos = 0;
        }

        // ---- expression = term (('+' | '-') term)* ---- //

        double parseExpression() {
            double result = parseTerm();
            while (pos < input.length()) {
                final char currentChar = input.charAt(pos);
                if (currentChar == PLUS) {
                    pos++;
                    result += parseTerm();
                } else if (currentChar == MINUS) {
                    pos++;
                    result -= parseTerm();
                } else {
                    break;
                }
            }
            return result;
        }

        // ---- term = power (('*' | '/' | '%') power)* ---- //

        private double parseTerm() {
            double result = parsePower();
            while (pos < input.length()) {
                final char currentChar = input.charAt(pos);
                if (currentChar == MULTIPLY) {
                    pos++;
                    result *= parsePower();
                } else if (currentChar == DIVIDE) {
                    pos++;
                    result /= parsePower();
                } else if (currentChar == MODULO) {
                    pos++;
                    result %= parsePower();
                } else {
                    break;
                }
            }
            return result;
        }

        // ---- power = unary ('^' power)?  (right-associative) ---- //

        private double parsePower() {
            final double base = parseUnary();
            final double result;
            if (pos < input.length()
                    && input.charAt(pos) == POWER) {
                pos++;
                final double exponent = parsePower();
                result = StrictMath.pow(base, exponent);
            } else {
                result = base;
            }
            return result;
        }

        // ---- unary = '-' unary | primary ---- //

        private double parseUnary() {
            final double result;
            if (pos < input.length()
                    && input.charAt(pos) == MINUS) {
                pos++;
                result = -parseUnary();
            } else {
                result = parsePrimary();
            }
            return result;
        }

        // ---- primary = NUMBER | VAR | FUNC '(' expr ')' | '(' expr ')' //

        private double parsePrimary() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException(
                        "Unexpected end of expression");
            }

            final char currentChar = input.charAt(pos);
            final double result;

            if (currentChar == OPEN_PAREN) {
                pos++;
                result = parseExpression();
                expectCloseParen();
            } else if (Character.isDigit(currentChar)
                    || currentChar == DOT) {
                result = parseNumber();
            } else if (Character.isLetter(currentChar)
                    || currentChar == UNDERSCORE) {
                result = parseIdentifier();
            } else {
                throw new IllegalArgumentException(
                        "Unexpected character at position " + pos
                                + ": '" + currentChar + "'");
            }
            return result;
        }

        // ---- number parsing (decimal + optional exponent) ---- //

        private double parseNumber() {
            final int start = pos;
            consumeDigits();
            if (pos < input.length()
                    && input.charAt(pos) == DOT) {
                pos++;
                consumeDigits();
            }
            if (pos < input.length()
                    && EXP_CHARS.indexOf(
                            input.charAt(pos)) >= 0) {
                pos++;
                if (pos < input.length()
                        && SIGN_CHARS.indexOf(
                                input.charAt(pos)) >= 0) {
                    pos++;
                }
                consumeDigits();
            }
            final String token = input.substring(start, pos);
            try {
                return Double.parseDouble(token);
            } catch (final NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Invalid number: " + token, ex);
            }
        }

        private void consumeDigits() {
            while (pos < input.length()
                    && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }

        // ---- identifier parsing (function call or variable) ---- //

        private double parseIdentifier() {
            final int start = pos;
            while (pos < input.length()
                    && (Character.isLetterOrDigit(
                            input.charAt(pos))
                    || input.charAt(pos) == UNDERSCORE)) {
                pos++;
            }
            final String name = input.substring(start, pos);

            final double result;
            if (pos < input.length()
                    && input.charAt(pos) == OPEN_PAREN) {
                pos++;
                final double argument = parseExpression();
                expectCloseParen();
                result = callFunction(name, argument);
            } else if (variables.containsKey(name)) {
                result = variables.get(name);
            } else {
                throw new IllegalArgumentException(
                        "Unknown variable: " + name);
            }
            return result;
        }

        private static double callFunction(
                final String name, final double arg) {

            final DoubleUnaryOperator operator =
                    FUNCTIONS.get(name);
            if (operator == null) {
                throw new IllegalArgumentException(
                        "Unknown function: " + name);
            }
            return operator.applyAsDouble(arg);
        }

        // ---- helpers ---- //

        private void expectCloseParen() {
            if (pos >= input.length()
                    || input.charAt(pos) != CLOSE_PAREN) {
                throw new IllegalArgumentException(
                        "Expected '" + CLOSE_PAREN
                                + "' at position " + pos);
            }
            pos++;
        }
    }
}
