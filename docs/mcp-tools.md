# MCP Tools Reference

Complete reference for all MCP tools exposed by the math-calculator server.

## Basic Calculator

Arbitrary-precision arithmetic using `BigDecimal`. All parameters and return values are strings.

### add

Add two numbers.

- **Params**: `a` (decimal string), `b` (decimal string)
- **Returns**: Sum as plain string
- **Example**: `add("1.5", "2.5")` → `"4.0"`

### subtract

Subtract the second number from the first.

- **Params**: `a` (decimal string), `b` (decimal string)
- **Returns**: Difference as plain string
- **Example**: `subtract("10", "3")` → `"7"`

### multiply

Multiply two numbers.

- **Params**: `a` (decimal string), `b` (decimal string)
- **Returns**: Product as plain string
- **Example**: `multiply("3", "4")` → `"12"`

### divide

Divide with up to 20 decimal places.

- **Params**: `a` (decimal string), `b` (decimal string, non-zero)
- **Returns**: Quotient as plain string
- **Throws**: `IllegalArgumentException` if `b` is zero
- **Example**: `divide("10", "3")` → `"3.33333333333333333333"`

### power

Raise base to an integer exponent.

- **Params**: `base` (decimal string), `exponent` (non-negative integer string)
- **Returns**: Result as plain string
- **Throws**: `IllegalArgumentException` if exponent is negative
- **Example**: `power("2", "10")` → `"1024"`

### modulo

Compute remainder.

- **Params**: `a` (decimal string), `b` (decimal string, non-zero)
- **Returns**: Remainder as plain string
- **Throws**: `IllegalArgumentException` if `b` is zero
- **Example**: `modulo("10", "3")` → `"1"`

### abs

Absolute value.

- **Params**: `a` (decimal string)
- **Returns**: Absolute value as plain string
- **Example**: `abs("-5")` → `"5"`

---

## Scientific Calculator

Uses `StrictMath` for reproducible results. Trig functions accept degrees.

### sqrt

Square root.

- **Params**: `number` (double, non-negative)
- **Returns**: `double`
- **Throws**: `IllegalArgumentException` if negative
- **Example**: `sqrt(16.0)` → `4.0`

### log

Natural logarithm (base e).

- **Params**: `number` (double, positive)
- **Returns**: `double`
- **Throws**: `IllegalArgumentException` if non-positive

### log10

Base-10 logarithm.

- **Params**: `number` (double, positive)
- **Returns**: `double`
- **Throws**: `IllegalArgumentException` if non-positive

### factorial

Factorial (0! to 20!).

- **Params**: `n` (int, 0-20)
- **Returns**: `String` (BigInteger result)
- **Throws**: `IllegalArgumentException` if out of range
- **Example**: `factorial(5)` → `"120"`

### sin

Sine of angle in degrees.

- **Params**: `degrees` (double)
- **Returns**: `double`
- **Example**: `sin(90.0)` → `1.0`

### cos

Cosine of angle in degrees.

- **Params**: `degrees` (double)
- **Returns**: `double`

### tan

Tangent of angle in degrees.

- **Params**: `degrees` (double)
- **Returns**: `double`

---

## Vector Calculator (SIMD)

Hardware-accelerated batch operations using Java 25 Vector API. Input arrays are comma-separated strings.

### sumArray

Sum all elements using SIMD vectorized addition.

- **Params**: `numbers` (comma-separated doubles)
- **Returns**: Sum as string
- **Example**: `sumArray("1.0,2.0,3.0")` → `"6.0"`

### dotProduct

Dot product of two arrays.

- **Params**: `a` (comma-separated), `b` (comma-separated, same length)
- **Returns**: Dot product as string
- **Throws**: `IllegalArgumentException` if lengths differ
- **Example**: `dotProduct("1,2,3", "4,5,6")` → `"32.0"`

### scaleArray

Multiply all elements by a scalar.

- **Params**: `numbers` (comma-separated), `scalar` (decimal string)
- **Returns**: Scaled array as comma-separated string
- **Example**: `scaleArray("1.0,2.0,3.0", "2.0")` → `"2.0,4.0,6.0"`

### magnitudeArray

Euclidean norm (magnitude) of a vector.

- **Params**: `numbers` (comma-separated doubles)
- **Returns**: Magnitude as string
- **Example**: `magnitudeArray("3.0,4.0")` → `"5.0"`

---

## Graphing Calculator

Mathematical analysis using the expression engine.

### plotFunction

Generate plot points for a function.

- **Params**: `expression`, `variable`, `min` (double), `max` (double), `steps` (int > 0)
- **Returns**: JSON array of `{"x":..., "y":...}` objects
- **Example**: `plotFunction("x^2", "x", -5.0, 5.0, 10)`

### solveEquation

Find a root using Newton-Raphson method.

- **Params**: `expression`, `variable`, `initialGuess` (double)
- **Returns**: Root value as string
- **Throws**: `IllegalArgumentException` if doesn't converge
- **Example**: `solveEquation("x^2 - 4", "x", 3.0)` → `"2.0"`

### findRoots

Find all roots in an interval using bisection.

- **Params**: `expression`, `variable`, `min` (double), `max` (double)
- **Returns**: JSON array of root values
- **Example**: `findRoots("x^2 - 4", "x", -5.0, 5.0)` → `[-2.0, 2.0]`

---

## Financial Calculator

All calculations use `BigDecimal` with `MathContext.DECIMAL128`. Rates are percentages.

### compoundInterest

Compound interest: `P * (1 + r/n)^(n*t)`.

- **Params**: `principal`, `annualRate` (%), `years`, `compoundsPerYear`
- **Returns**: Final amount as string

### loanPayment

Fixed monthly loan payment (annuity formula).

- **Params**: `principal`, `annualRate` (%), `years`
- **Returns**: Monthly payment as string

### presentValue

Present value of a future amount.

- **Params**: `futureValue`, `annualRate` (%), `years`
- **Returns**: Present value as string

### futureValueAnnuity

Future value of periodic payments.

- **Params**: `payment`, `annualRate` (%), `years`
- **Returns**: Future value as string

### returnOnInvestment

ROI as a percentage.

- **Params**: `gain`, `cost` (non-zero)
- **Returns**: ROI percentage as string

### amortizationSchedule

Full monthly amortization schedule.

- **Params**: `principal`, `annualRate` (%), `years`
- **Returns**: JSON array of `{month, payment, principal, interest, balance}`

---

## Printing Calculator

Simulates a physical printing/tape calculator.

### calculateWithTape

Process a sequence of operations and return a formatted tape.

- **Params**: `operations` — JSON array of `{"op":"...", "value":"..."}`
- **Supported ops**: `+`, `-`, `*`, `/`, `=` (subtotal), `C` (clear), `T` (grand total)
- **Returns**: Multi-line tape string

---

## Programmable Calculator

Expression evaluation using the built-in recursive descent parser.

### evaluate

Evaluate a mathematical expression.

- **Params**: `expression` — supports `+`, `-`, `*`, `/`, `^`, `%`, parentheses, functions
- **Returns**: Result as string
- **Functions**: `sin`, `cos`, `tan`, `log`, `log10`, `sqrt`, `abs`, `ceil`, `floor`

### evaluateWithVariables

Evaluate with variable substitution.

- **Params**: `expression`, `variables` (JSON map, e.g. `{"x": 3.0}`)
- **Returns**: Result as string
