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

Uses `StrictMath` for reproducible results. All methods return `String`. Invalid inputs return `"Error: ..."` messages instead of throwing exceptions, making errors easy for LLMs to parse.

Trig functions use precomputed lookup tables for exact results at notable angles (multiples of 30 and 45 degrees). Non-notable angles fall back to `StrictMath`.

### sqrt

Compute square root of a number.

- **Params**: `number` (double, non-negative)
- **Returns**: `String` — result or `"Error: ..."` if negative
- **Example**: `sqrt(16.0)` -> `"4.0"`
- **Error**: `sqrt(-1)` -> `"Error: Square root is undefined for negative numbers. Received: -1.0"`

### log

Compute natural logarithm (ln) of a number.

- **Params**: `number` (double, positive)
- **Returns**: `String` — result or `"Error: ..."` if non-positive
- **Example**: `log(2.718281828459045)` -> `"1.0"`

### log10

Compute base-10 logarithm of a number.

- **Params**: `number` (double, positive)
- **Returns**: `String` — result or `"Error: ..."` if non-positive
- **Example**: `log10(100)` -> `"2.0"`

### factorial

Compute factorial (n!). Range: 0 to 20.

- **Params**: `num` (int, 0–20)
- **Returns**: `String` — result or `"Error: ..."` if out of range
- **Example**: `factorial(5)` -> `"120"`

### sin

Compute sine of an angle in degrees. Exact at notable angles (0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330).

- **Params**: `degrees` (double)
- **Returns**: `String`
- **Example**: `sin(30)` -> `"0.5"`, `sin(90)` -> `"1.0"`, `sin(360)` -> `"0.0"`

### cos

Compute cosine of an angle in degrees. Exact at notable angles.

- **Params**: `degrees` (double)
- **Returns**: `String`
- **Example**: `cos(60)` -> `"0.5"`, `cos(90)` -> `"0.0"`

### tan

Compute tangent of an angle in degrees. Returns error at vertical asymptotes (90, 270, etc.).

- **Params**: `degrees` (double)
- **Returns**: `String` — result or `"Error: ..."` at undefined angles
- **Example**: `tan(45)` -> `"1.0"`
- **Error**: `tan(90)` -> `"Error: Tangent is undefined at 90 degrees (vertical asymptote)."`

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

Plot a function. Returns JSON array of {x, y} points. Uses `BigDecimal` step arithmetic to prevent floating-point drift in x-values.

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

---

## Unit Converter

General-purpose unit conversion across 21 measurement categories. Delegates to `UnitRegistry`.

### convert

Convert a value between measurement units.

- **Params**: `value` (decimal string), `fromUnit` (unit code), `toUnit` (unit code), `category` (category name)
- **Returns**: Converted value as string
- **Error**: Returns `"Error: ..."` for unknown units, wrong category, or cross-category conversion
- **Example**: `convert("100", "c", "f", "TEMPERATURE")` -> `"212"`

### convertAutoDetect

Convert units with auto-detected category.

- **Params**: `value` (decimal string), `fromUnit` (unit code), `toUnit` (unit code)
- **Returns**: Converted value as string
- **Error**: Returns `"Error: ..."` for unknown units or cross-category conversion
- **Example**: `convertAutoDetect("1", "km", "mi")` -> `"0.621371..."`

---

## Cooking Converter

Specialized subset of the unit registry for cooking contexts. Includes gas mark support.

### convertCookingVolume

Convert cooking volume units.

- **Params**: `value` (decimal string), `fromUnit`, `toUnit`
- **Allowed units**: `l`, `ml`, `uscup`, `tbsp`, `tsp`, `usfloz`, `usgal`, `igal`
- **Returns**: Converted value as string
- **Error**: Returns `"Error: ..."` for non-cooking units
- **Example**: `convertCookingVolume("1", "uscup", "tbsp")` -> `"16"`

### convertCookingWeight

Convert cooking weight units.

- **Params**: `value` (decimal string), `fromUnit`, `toUnit`
- **Allowed units**: `kg`, `g`, `mg`, `lb`, `oz`
- **Returns**: Converted value as string
- **Error**: Returns `"Error: ..."` for non-cooking units
- **Example**: `convertCookingWeight("1", "lb", "oz")` -> `"16"`

### convertOvenTemperature

Convert oven temperature units.

- **Params**: `value` (decimal string), `fromUnit`, `toUnit`
- **Supported units**: `c` (Celsius), `f` (Fahrenheit), `gasmark` (Gas Mark 1–10)
- **Returns**: Converted value as string
- **Gas mark**: Uses lookup table (not linear). Mark 1 = 140 C, Mark 10 = 260 C
- **Example**: `convertOvenTemperature("4", "gasmark", "c")` -> `"180"`

---

## Measure Reference

Lookup and reference methods for the unit registry.

### listCategories

List all unit conversion categories.

- **Params**: none
- **Returns**: JSON array of `{"name":"CATEGORY_NAME"}` objects
- **Example**: `listCategories()` -> `[{"name":"DATA_STORAGE"},{"name":"LENGTH"},...]`

### listUnits

List units in a category.

- **Params**: `category` (category name)
- **Returns**: JSON array of `{"code":"...","name":"..."}` objects
- **Error**: Returns `"Error: ..."` for invalid category
- **Example**: `listUnits("LENGTH")` -> `[{"code":"m","name":"meter"},...]`

### getConversionFactor

Get conversion factor between two units.

- **Params**: `fromUnit` (unit code), `toUnit` (unit code)
- **Returns**: Multiplicative factor as string
- **Error**: Returns `"Error: ..."` for temperature (formula-based) or cross-category
- **Example**: `getConversionFactor("km", "m")` -> `"1000"`

### explainConversion

Explain conversion formula between units.

- **Params**: `fromUnit` (unit code), `toUnit` (unit code)
- **Returns**: Human-readable explanation string
- **Example**: `explainConversion("c", "f")` -> includes `"F = C * 9/5 + 32"`

---

## DateTime Converter

Timezone conversion, format transformation, and time difference calculation using the `java.time` API with IANA timezone identifiers.

### convertTimezone

Convert datetime between timezones.

- **Params**: `datetime` (ISO-8601 string), `fromTimezone` (IANA timezone), `toTimezone` (IANA timezone)
- **Returns**: Converted datetime as zoned ISO string
- **Error**: Returns `"Error: ..."` for invalid timezone or unparseable datetime
- **Example**: `convertTimezone("2026-03-03T12:00:00", "UTC", "Asia/Tokyo")` -> `"2026-03-03T21:00:00+09:00[Asia/Tokyo]"`

### formatDateTime

Reformat a datetime string.

- **Params**: `datetime`, `inputFormat`, `outputFormat`, `timezone`
- **Input keywords**: `iso`, `epoch`, `epochmillis`, or Java `DateTimeFormatter` pattern
- **Output keywords**: `iso`, `iso-offset`, `iso-local`, `epoch`, `epochmillis`, `rfc1123`, or pattern
- **Returns**: Reformatted datetime string
- **Example**: `formatDateTime("1709424000", "epoch", "iso", "UTC")` -> `"2024-03-03T00:00:00Z[UTC]"`

### currentDateTime

Get current datetime in a timezone.

- **Params**: `timezone` (IANA timezone), `format` (output format keyword or pattern)
- **Returns**: Current datetime as formatted string
- **Example**: `currentDateTime("UTC", "iso")` -> `"2026-03-04T12:00:00Z[UTC]"`

### listTimezones

List timezone IDs by region.

- **Params**: `region` (e.g., `America`, `Europe`, `Asia`)
- **Returns**: JSON array of timezone ID strings
- **Example**: `listTimezones("America")` -> `["America/New_York","America/Sao_Paulo",...]`

### dateTimeDifference

Calculate time difference between two datetimes.

- **Params**: `datetime1`, `datetime2`, `timezone` (for parsing)
- **Returns**: JSON with `years`, `months`, `days`, `hours`, `minutes`, `seconds`, `totalSeconds`
- **Example**: `dateTimeDifference("2026-01-01T00:00:00", "2026-03-03T15:30:00", "UTC")` -> `{"years":0,"months":2,"days":2,...}`

---

## Network Calculator

Dual-stack IPv4/IPv6 networking tools. Uses `UnitRegistry` for DATA_STORAGE and DATA_RATE conversions.

### subnetCalculator

Calculate subnet details from CIDR notation.

- **Params**: `cidr` (string, e.g. `"192.168.1.0/24"`)
- **Returns**: JSON with network address, broadcast, first/last host, total hosts, subnet mask, wildcard

### ipToBinary

Convert an IP address to binary representation.

- **Params**: `ip` (string, IPv4 or IPv6)
- **Returns**: Binary string with dot/colon separators

### binaryToIp

Convert a binary representation back to an IP address.

- **Params**: `binary` (binary string)
- **Returns**: IP address string

### ipToDecimal

Convert an IP address to its decimal (integer) representation.

- **Params**: `ip` (string, IPv4 or IPv6)
- **Returns**: Decimal string

### decimalToIp

Convert a decimal (integer) representation to an IP address.

- **Params**: `decimal` (string), `version` (string, `"4"` or `"6"`)
- **Returns**: IP address string

### ipInSubnet

Check whether an IP address falls within a given subnet.

- **Params**: `ip` (string), `cidr` (string)
- **Returns**: `"true"` or `"false"`

### vlsmSubnets

Perform Variable Length Subnet Masking. Divide a network into subnets of specified sizes.

- **Params**: `cidr` (string), `sizes` (JSON array of host counts)
- **Returns**: JSON array of subnet allocations

### summarizeSubnets

Summarize (supernet) a list of contiguous subnets into the smallest CIDR.

- **Params**: `subnets` (JSON array of CIDR strings)
- **Returns**: Summarized CIDR string

### expandIpv6

Expand a compressed IPv6 address to full notation.

- **Params**: `ipv6` (string)
- **Returns**: Fully expanded IPv6 string

### compressIpv6

Compress an IPv6 address to shortest notation.

- **Params**: `ipv6` (string)
- **Returns**: Compressed IPv6 string

### transferTime

Calculate file transfer time given size and bandwidth.

- **Params**: `size` (string), `sizeUnit` (unit code), `bandwidth` (string), `bandwidthUnit` (unit code)
- **Returns**: Transfer time as human-readable string

### throughput

Calculate throughput from transfer size and elapsed time.

- **Params**: `size` (string), `sizeUnit` (unit code), `seconds` (string)
- **Returns**: Throughput as string with unit

### tcpThroughput

Estimate maximum TCP throughput using the bandwidth-delay product formula.

- **Params**: `bandwidthMbps` (string), `rttMs` (string), `windowSizeKb` (string)
- **Returns**: JSON with theoretical max throughput and utilization percentage

---

## Analog Electronics

Analog circuit analysis tools. Uses `BigDecimal` with `DECIMAL128` precision and `StrictMath` for transcendental functions.

### ohmsLaw

Calculate voltage, current, or resistance using Ohm's law (V = I * R). Provide any two, leave the unknown empty.

- **Params**: `voltage` (string, optional), `current` (string, optional), `resistance` (string, optional)
- **Returns**: JSON with all three values

### resistorCombination

Calculate equivalent resistance of resistors in series or parallel.

- **Params**: `resistors` (comma-separated values), `mode` (string, `"series"` or `"parallel"`)
- **Returns**: Equivalent resistance as string

### capacitorCombination

Calculate equivalent capacitance of capacitors in series or parallel.

- **Params**: `capacitors` (comma-separated values), `mode` (string, `"series"` or `"parallel"`)
- **Returns**: Equivalent capacitance as string

### inductorCombination

Calculate equivalent inductance of inductors in series or parallel.

- **Params**: `inductors` (comma-separated values), `mode` (string, `"series"` or `"parallel"`)
- **Returns**: Equivalent inductance as string

### voltageDivider

Calculate output voltage of a resistive voltage divider.

- **Params**: `vin` (string), `r1` (string), `r2` (string)
- **Returns**: Output voltage as string

### currentDivider

Calculate branch current in a current divider.

- **Params**: `totalCurrent` (string), `branchResistance` (string), `totalResistance` (string)
- **Returns**: Branch current as string

### rcTimeConstant

Calculate RC circuit time constant and charge/discharge times.

- **Params**: `resistance` (string), `capacitance` (string)
- **Returns**: JSON with tau, t63, t95, t99

### rlTimeConstant

Calculate RL circuit time constant.

- **Params**: `resistance` (string), `inductance` (string)
- **Returns**: JSON with tau, t63, t95, t99

### rlcResonance

Calculate resonant frequency and quality factor of an RLC circuit.

- **Params**: `resistance` (string), `inductance` (string), `capacitance` (string)
- **Returns**: JSON with resonant frequency, quality factor, bandwidth

### impedance

Calculate complex impedance of R, L, C components at a given frequency.

- **Params**: `resistance` (string), `inductance` (string), `capacitance` (string), `frequency` (string)
- **Returns**: JSON with magnitude, phase angle, real, imaginary parts

### decibelConvert

Convert between linear and decibel scales (power or voltage).

- **Params**: `value` (string), `mode` (string, `"toDb"` or `"fromDb"`), `type` (string, `"power"` or `"voltage"`)
- **Returns**: Converted value as string

### filterCutoff

Calculate cutoff frequency of an RC or RL filter.

- **Params**: `resistance` (string), `reactiveComponent` (string), `type` (string, `"rc"` or `"rl"`)
- **Returns**: Cutoff frequency as string

### ledResistor

Calculate the required current-limiting resistor for an LED.

- **Params**: `supplyVoltage` (string), `ledForwardVoltage` (string), `ledCurrent` (string)
- **Returns**: Resistor value as string

### wheatstoneBridge

Analyze a Wheatstone bridge circuit.

- **Params**: `r1` (string), `r2` (string), `r3` (string), `r4` (string, optional for unknown)
- **Returns**: JSON with balance status, unknown resistance, or bridge voltage

---

## Digital Electronics

Digital circuit and signal processing tools. Uses `BigInteger` for arbitrary-precision base conversion.

### convertBase

Convert a number between bases (2, 8, 10, 16, or any base 2-36).

- **Params**: `value` (string), `fromBase` (int), `toBase` (int)
- **Returns**: Converted value as string

### twosComplement

Compute two's complement representation of a signed integer.

- **Params**: `value` (string), `bits` (int)
- **Returns**: Binary two's complement string

### grayCode

Convert between binary and Gray code.

- **Params**: `value` (string), `mode` (string, `"toGray"` or `"fromGray"`)
- **Returns**: Converted code as string

### bitwiseOp

Perform bitwise operations (AND, OR, XOR, NOT, NAND, NOR, XNOR, shift).

- **Params**: `a` (string), `b` (string, optional for NOT), `operation` (string)
- **Returns**: Result as string in the same base as input

### adcResolution

Calculate ADC resolution, step size, and quantization error.

- **Params**: `bits` (int), `referenceVoltage` (string)
- **Returns**: JSON with levels, step size, quantization error

### dacOutput

Calculate DAC output voltage from a digital input value.

- **Params**: `digitalValue` (string), `bits` (int), `referenceVoltage` (string)
- **Returns**: Output voltage as string

### timer555Astable

Calculate 555 timer astable mode parameters.

- **Params**: `r1` (string, ohms), `r2` (string, ohms), `capacitance` (string, farads)
- **Returns**: JSON with frequency, period, duty cycle, high/low times

### timer555Monostable

Calculate 555 timer monostable mode pulse duration.

- **Params**: `resistance` (string, ohms), `capacitance` (string, farads)
- **Returns**: Pulse duration as string

### frequencyPeriod

Convert between frequency and period.

- **Params**: `value` (string), `mode` (string, `"toPeriod"` or `"toFrequency"`)
- **Returns**: Converted value as string

### nyquistRate

Calculate the Nyquist rate for a given signal frequency.

- **Params**: `signalFrequency` (string)
- **Returns**: JSON with Nyquist rate and Nyquist frequency

---

## Calculus

Numerical calculus tools using the `ExpressionEvaluator`. Derivatives use five-point central difference; integrals use composite Simpson's rule.

### derivative

Compute the first derivative of an expression at a point.

- **Params**: `expression` (string), `variable` (string), `point` (double)
- **Returns**: Derivative value as string
- **Example**: `derivative("x^2", "x", 3.0)` -> `"6.0"`

### nthDerivative

Compute the nth derivative of an expression at a point.

- **Params**: `expression` (string), `variable` (string), `point` (double), `order` (int)
- **Returns**: Nth derivative value as string
- **Example**: `nthDerivative("x^3", "x", 2.0, 2)` -> `"12.0"`

### definiteIntegral

Compute the definite integral of an expression over an interval.

- **Params**: `expression` (string), `variable` (string), `lower` (double), `upper` (double)
- **Returns**: Integral value as string
- **Example**: `definiteIntegral("x^2", "x", 0.0, 1.0)` -> `"0.333333..."`

### tangentLine

Compute the equation of the tangent line to a curve at a point.

- **Params**: `expression` (string), `variable` (string), `point` (double)
- **Returns**: Tangent line equation as string (e.g. `"y = 6.0x + -9.0"`)
