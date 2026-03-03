# MCP Tools Reference

Complete reference for all MCP tools exposed by the math-calculator server.

## Basic Calculator

Arbitrary-precision arithmetic using `BigDecimal`. All parameters and return values are strings.

### add

Add two numbers. Returns exact result.

- **Params**: `first` (decimal string), `second` (decimal string)
- **Returns**: Sum as plain string
- **Example**: `add("1.5", "2.5")` -> `"4.0"`

### subtract

Subtract second from first. Returns exact result.

- **Params**: `first` (decimal string), `second` (decimal string)
- **Returns**: Difference as plain string
- **Example**: `subtract("10", "3")` -> `"7"`

### multiply

Multiply two numbers. Returns exact result.

- **Params**: `first` (decimal string), `second` (decimal string)
- **Returns**: Product as plain string
- **Example**: `multiply("3", "4")` -> `"12"`

### divide

Divide first by second. 20-digit precision.

- **Params**: `first` (decimal string), `second` (decimal string, non-zero)
- **Returns**: Quotient as plain string
- **Throws**: `IllegalArgumentException` if `second` is zero
- **Example**: `divide("10", "3")` -> `"3.33333333333333333333"`

### power

Raise base to exponent. Returns exact result.

- **Params**: `base` (decimal string), `exponent` (non-negative integer string)
- **Returns**: Result as plain string
- **Throws**: `IllegalArgumentException` if exponent is negative
- **Example**: `power("2", "10")` -> `"1024"`

### modulo

Compute remainder of first divided by second.

- **Params**: `first` (decimal string), `second` (decimal string, non-zero)
- **Returns**: Remainder as plain string
- **Throws**: `IllegalArgumentException` if `second` is zero
- **Example**: `modulo("10", "3")` -> `"1"`

### abs

Compute absolute value of a number.

- **Params**: `value` (decimal string)
- **Returns**: Absolute value as plain string
- **Example**: `abs("-5")` -> `"5"`

---

## Scientific Calculator

Uses `StrictMath` for reproducible results. Trig functions accept degrees.

### sqrt

Compute square root of a number.

- **Params**: `number` (double, non-negative)
- **Returns**: `double`
- **Throws**: `IllegalArgumentException` if negative
- **Example**: `sqrt(16.0)` -> `4.0`

### log

Compute natural logarithm (ln) of a number.

- **Params**: `number` (double, positive)
- **Returns**: `double`
- **Throws**: `IllegalArgumentException` if non-positive

### log10

Compute base-10 logarithm of a number.

- **Params**: `number` (double, positive)
- **Returns**: `double`
- **Throws**: `IllegalArgumentException` if non-positive

### factorial

Compute factorial (n!). Range: 0 to 20.

- **Params**: `num` (int, 0-20)
- **Returns**: `String` (BigInteger result)
- **Throws**: `IllegalArgumentException` if out of range
- **Example**: `factorial(5)` -> `"120"`

### sin

Compute sine of an angle in degrees.

- **Params**: `degrees` (double)
- **Returns**: `double`
- **Example**: `sin(90.0)` -> `1.0`

### cos

Compute cosine of an angle in degrees.

- **Params**: `degrees` (double)
- **Returns**: `double`

### tan

Compute tangent of an angle in degrees.

- **Params**: `degrees` (double)
- **Returns**: `double`

---

## Vector Calculator (SIMD)

Hardware-accelerated batch operations using Java 25 Vector API. Input arrays are comma-separated strings.

### sumArray

Sum all elements of a numeric array.

- **Params**: `numbers` (comma-separated doubles)
- **Returns**: Sum as string
- **Example**: `sumArray("1.0,2.0,3.0")` -> `"6.0"`

### dotProduct

Compute dot product of two numeric arrays.

- **Params**: `first` (comma-separated), `second` (comma-separated, same length)
- **Returns**: Dot product as string
- **Throws**: `IllegalArgumentException` if lengths differ
- **Example**: `dotProduct("1,2,3", "4,5,6")` -> `"32.0"`

### scaleArray

Multiply all array elements by a scalar.

- **Params**: `numbers` (comma-separated), `scalar` (decimal string)
- **Returns**: Scaled array as comma-separated string
- **Example**: `scaleArray("1.0,2.0,3.0", "2.0")` -> `"2.0,4.0,6.0"`

### magnitudeArray

Compute Euclidean norm (magnitude) of a vector.

- **Params**: `numbers` (comma-separated doubles)
- **Returns**: Magnitude as string
- **Example**: `magnitudeArray("3.0,4.0")` -> `"5.0"`

---

## Graphing Calculator

Mathematical analysis using the expression engine.

### plotFunction

Plot a function. Returns JSON array of {x, y} points.

- **Params**: `expression`, `variable`, `min` (double), `max` (double), `steps` (int > 0)
- **Returns**: JSON array of `{"x":..., "y":...}` objects
- **Example**: `plotFunction("x^2", "x", -5.0, 5.0, 10)`

### solveEquation

Solve f(x)=0 via Newton-Raphson. Returns root value.

- **Params**: `expression`, `variable`, `initialGuess` (double)
- **Returns**: Root value as string
- **Throws**: `IllegalArgumentException` if doesn't converge
- **Example**: `solveEquation("x^2 - 4", "x", 3.0)` -> `"2.0"`

### findRoots

Find all real roots of f(x)=0 in an interval.

- **Params**: `expression`, `variable`, `min` (double), `max` (double)
- **Returns**: JSON array of root values
- **Example**: `findRoots("x^2 - 4", "x", -5.0, 5.0)` -> `[-2.0, 2.0]`

---

## Financial Calculator

All calculations use `BigDecimal` with `MathContext.DECIMAL128`. Rates are percentages.

### compoundInterest

Compute compound interest. Returns final amount.

- **Params**: `principal` (String), `annualRate` (String, %), `years` (String), `compoundsPerYear` (int)
- **Returns**: Final amount as string

### loanPayment

Compute fixed monthly loan payment.

- **Params**: `principal` (String), `annualRate` (String, %), `years` (String)
- **Returns**: Monthly payment as string

### presentValue

Compute present value of a future amount.

- **Params**: `futureValue` (String), `annualRate` (String, %), `years` (String)
- **Returns**: Present value as string

### futureValueAnnuity

Compute future value of an ordinary annuity.

- **Params**: `payment` (String), `annualRate` (String, %), `years` (String)
- **Returns**: Future value as string

### returnOnInvestment

Compute ROI as a percentage.

- **Params**: `gain` (String), `cost` (String, non-zero)
- **Returns**: ROI percentage as string

### amortizationSchedule

Generate monthly amortization schedule as JSON.

- **Params**: `principal` (String), `annualRate` (String, %), `years` (String)
- **Returns**: JSON array of `{month, payment, principal, interest, balance}`

---

## Printing Calculator

Simulates a physical printing/tape calculator.

### calculateWithTape

Tape calculator. Returns printed tape with running totals.

- **Params**: `operations` -- JSON array of `{"op":"...", "value":"..."}`
- **Supported ops**: `+`, `-`, `*`, `/`, `=` (subtotal), `C` (clear), `T` (grand total)
- **Returns**: Multi-line tape string

---

## Programmable Calculator

Expression evaluation using the built-in recursive descent parser.

### evaluate

Evaluate a math expression. Supports +,-,*,/,^,% and functions.

- **Params**: `expression` -- supports `+`, `-`, `*`, `/`, `^`, `%`, parentheses, functions
- **Returns**: Result as string
- **Functions**: `sin`, `cos`, `tan`, `log`, `log10`, `sqrt`, `abs`, `ceil`, `floor`

### evaluateWithVariables

Evaluate a math expression with variables.

- **Params**: `expression`, `variables` (JSON map, e.g. `{"x": 3.0}`)
- **Returns**: Result as string
