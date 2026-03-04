# Unit Conversion Reference

## Overview

The unit conversion system supports **21 measurement categories** with precision-first design using `BigDecimal` and `MathContext.DECIMAL128` (34 significant digits).

## Architecture

- **Engine**: `UnitCategory` enum, `UnitDefinition` record, `UnitRegistry` static utility
- **Tools**: `UnitConverterTool`, `CookingConverterTool`, `MeasureReferenceTool`

### Conversion Algorithm

```
Linear:      result = value * from.toBaseFactor / to.toBaseFactor
Temperature: value -> toCelsius(value, from) -> fromCelsius(celsius, to)
Gas Mark:    lookup table (not linear)
```

## Categories and Unit Codes

### DATA_STORAGE (base: byte)

| Code | Name | Factor |
|------|------|--------|
| byte | byte | 1 |
| bit | bit | 0.125 |
| kb | kilobyte | 1024 |
| mb | megabyte | 1048576 |
| gb | gigabyte | 1073741824 |
| tb | terabyte | 1099511627776 |
| pb | petabyte | 1125899906842624 |

### LENGTH (base: meter)

| Code | Name | Factor |
|------|------|--------|
| m | meter | 1 |
| mm | millimeter | 0.001 |
| cm | centimeter | 0.01 |
| km | kilometer | 1000 |
| in | inch | 0.0254 (exact SI) |
| ft | foot | 0.3048 (exact SI) |
| yd | yard | 0.9144 |
| mi | mile | 1609.344 |
| nmi | nautical mile | 1852 |

### MASS (base: kilogram)

| Code | Name | Factor |
|------|------|--------|
| kg | kilogram | 1 |
| g | gram | 0.001 |
| mg | milligram | 0.000001 |
| t | tonne | 1000 |
| lb | pound | 0.45359237 (exact) |
| oz | ounce | 0.028349523125 |
| st | stone | 6.35029318 |

### VOLUME (base: liter)

| Code | Name | Factor |
|------|------|--------|
| l | liter | 1 |
| ml | milliliter | 0.001 |
| m3 | cubic meter | 1000 |
| usgal | US gallon | 3.785411784 |
| igal | imperial gallon | 4.54609 |
| uscup | US cup | 0.2365882365 |
| tbsp | tablespoon | 0.01478676478125 |
| tsp | teaspoon | 0.00492892159375 |
| usfloz | US fluid ounce | 0.0295735295625 |

### TEMPERATURE (base: Celsius, formula-based)

| Code | Name | Formula |
|------|------|---------|
| c | Celsius | base |
| f | Fahrenheit | F = C * 9/5 + 32 |
| k | Kelvin | K = C + 273.15 |
| r | Rankine | R = C * 9/5 + 491.67 |

### TIME (base: second)

| Code | Name | Factor |
|------|------|--------|
| s | second | 1 |
| ms | millisecond | 0.001 |
| min | minute | 60 |
| h | hour | 3600 |
| d | day | 86400 |
| wk | week | 604800 |
| yr | year | 31557600 (Julian) |

### SPEED (base: m/s)

| Code | Name | Factor |
|------|------|--------|
| m/s | meter per second | 1 |
| km/h | kilometer per hour | 1000/3600 (computed) |
| mph | mile per hour | 0.44704 (exact) |
| kn | knot | 1852/3600 (computed) |
| ft/s | foot per second | 0.3048 (exact) |

### AREA (base: m²)

| Code | Name | Factor |
|------|------|--------|
| m2 | square meter | 1 |
| cm2 | square centimeter | 0.0001 |
| km2 | square kilometer | 1000000 |
| ft2 | square foot | 0.09290304 |
| ac | acre | 4046.8564224 |
| ha | hectare | 10000 |
| mi2 | square mile | 2589988.110336 |

### ENERGY (base: joule)

| Code | Name | Factor |
|------|------|--------|
| j | joule | 1 |
| cal | calorie | 4.184 |
| kcal | kilocalorie | 4184 |
| kwh | kilowatt-hour | 3600000 |
| btu | BTU | 1055.05585262 |
| ev | electronvolt | 1.602176634E-19 |

### FORCE (base: newton)

| Code | Name | Factor |
|------|------|--------|
| n | newton | 1 |
| dyn | dyne | 0.00001 |
| lbf | pound-force | derived (lb * g) |
| kgf | kilogram-force | 9.80665 |

### PRESSURE (base: pascal)

| Code | Name | Factor |
|------|------|--------|
| pa | pascal | 1 |
| bar | bar | 100000 |
| atm | atmosphere | 101325 |
| psi | pound per square inch | derived (lbf/in²) |
| torr | torr | 101325/760 (computed) |
| mmhg | millimeter of mercury | 133.322387415 |

### POWER (base: watt)

| Code | Name | Factor |
|------|------|--------|
| w | watt | 1 |
| kw | kilowatt | 1000 |
| hp | horsepower | derived (550 * ft * lbf) |
| btu/h | BTU per hour | derived (btu/3600) |

### DENSITY (base: kg/m³)

| Code | Name | Factor |
|------|------|--------|
| kg/m3 | kilogram per cubic meter | 1 |
| g/cm3 | gram per cubic centimeter | 1000 |
| g/ml | gram per milliliter | 1000 |
| lb/ft3 | pound per cubic foot | 16.018463374 |

### FREQUENCY (base: hertz)

| Code | Name | Factor |
|------|------|--------|
| hz | hertz | 1 |
| khz | kilohertz | 1000 |
| mhz | megahertz | 1000000 |
| ghz | gigahertz | 1000000000 |
| rpm | revolutions per minute | 1/60 (computed) |

### ANGLE (base: degree)

| Code | Name | Factor |
|------|------|--------|
| deg | degree | 1 |
| rad | radian | 180/π (computed, 40+ digit π) |
| grad | gradian | 0.9 |
| arcmin | arcminute | 1/60 (computed) |
| arcsec | arcsecond | 1/3600 (computed) |
| turn | turn | 360 |

### DATA_RATE (base: bit per second)

| Code | Name | Factor |
|------|------|--------|
| bps | bit per second | 1 |
| kbps | kilobit per second | 1000 |
| mbps | megabit per second | 1000000 |
| gbps | gigabit per second | 1000000000 |
| tbps | terabit per second | 1000000000000 |
| byps | byte per second | 8 |
| kbyps | kilobyte per second | 8000 |
| mbyps | megabyte per second | 8000000 |
| gbyps | gigabyte per second | 8000000000 |

### RESISTANCE (base: ohm)

| Code | Name | Factor |
|------|------|--------|
| ohm | ohm | 1 |
| mohm | milliohm | 0.001 |
| kohm | kilohm | 1000 |
| megohm | megaohm | 1000000 |

### CAPACITANCE (base: farad)

| Code | Name | Factor |
|------|------|--------|
| fd | farad | 1 |
| mfd | millifarad | 0.001 |
| uf | microfarad | 0.000001 |
| nf | nanofarad | 0.000000001 |
| pf | picofarad | 0.000000000001 |

### INDUCTANCE (base: henry)

| Code | Name | Factor |
|------|------|--------|
| hy | henry | 1 |
| mhy | millihenry | 0.001 |
| uhy | microhenry | 0.000001 |
| nhy | nanohenry | 0.000000001 |

### VOLTAGE (base: volt)

| Code | Name | Factor |
|------|------|--------|
| vlt | volt | 1 |
| mvlt | millivolt | 0.001 |
| kvlt | kilovolt | 1000 |
| uvlt | microvolt | 0.000001 |

### CURRENT (base: ampere)

| Code | Name | Factor |
|------|------|--------|
| amp | ampere | 1 |
| mamp | milliampere | 0.001 |
| uamp | microampere | 0.000001 |
| namp | nanoampere | 0.000000001 |

## Gas Mark Table

| Gas Mark | Celsius |
|----------|---------|
| 1 | 140 |
| 2 | 150 |
| 3 | 170 |
| 4 | 180 |
| 5 | 190 |
| 6 | 200 |
| 7 | 220 |
| 8 | 230 |
| 9 | 240 |
| 10 | 260 |

## Precision Notes

- All factors stored as `BigDecimal` string literals from NIST/SI definitions
- Non-terminating factors computed as exact fractions at class-load time with `DECIMAL128`
- Internal scale: 34 digits, `RoundingMode.HALF_UP`
- Output: `stripTrailingZeros().toPlainString()`

## MCP Tool Examples

```
convert("100", "c", "f", "TEMPERATURE")        -> "212"
convertAutoDetect("1", "km", "mi")              -> "0.621371..."
convertCookingVolume("1", "uscup", "tbsp")      -> "16"
convertOvenTemperature("4", "gasmark", "c")     -> "180"
listCategories()                                -> [{"name":"DATA_STORAGE"},...]
listUnits("LENGTH")                             -> [{"code":"m","name":"meter"},...]
getConversionFactor("km", "mi")                 -> "0.621371..."
explainConversion("c", "f")                     -> "F = C * 9/5 + 32"
```
