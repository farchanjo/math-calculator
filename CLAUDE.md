# Math Calculator — Project Instructions

## Language

- **en-US for ALL written artifacts**: code, comments, javadoc, commits, logs, docs, README, branch names, PR descriptions
- **Console responses**: reply in the same language the user used in their message

## Build & Run

```bash
./gradlew build        # compile + test + lint
./gradlew bootRun      # start on port 44321
./gradlew test         # tests only

# MCP integration tests (requires bootRun on port 44321)
python3 scripts/mcp_test.py        # Streamable HTTP — full suite (320 tests)
python3 scripts/mcp_sse_test.py    # dual-transport — SSE + Streamable HTTP (26 tests)
```

## Conventions

- **Package**: `com.archanjo.mathcalculator`
- **Group**: `com.archanjo`
- **Java**: 25 (toolchain)
- **Port**: 44321
- **Transport**: Dual — Streamable HTTP (`POST /mcp`) + SSE (`GET /sse`, `POST /mcp/message`) via WebFlux + Netty
- **MCP type**: SYNC

## Code Style

- Angular commits: `<type>(<scope>): <subject>`
- Primitives over wrappers (`double` not `Double`)
- Methods under 30 lines
- `BigDecimal` for basic/financial/graphing precision
- `StrictMath` for scientific determinism + notable-angle lookup tables for exact trig values
- Scientific tools return `String` — errors as `"Error: ..."` messages (no exceptions)
- `@Tool` / `@ToolParam` from `org.springframework.ai.tool.annotation`
- SIMD via `jdk.incubator.vector` (requires `--add-modules`)
- Unit codes are lowercase (e.g., `km`, `lb`, `c`, `psi`)
- Conversion factors: exact SI definitions, non-terminating factors computed as fractions with DECIMAL128
- DateTime: java.time API, IANA timezone IDs, ISO-8601 default format

## Linting

- **Checkstyle 10.23.1**: `checkstyle.xml` -- zero warnings, zero errors
- **PMD 7.22.0**: `pmd-rules.xml` -- console output
  - Excluded rules: `UnnecessaryConstructor`, `AtLeastOneConstructor`, `CommentDefaultAccessModifier`
- **Compiler**: `-Xlint:all -Xlint:-incubating -Werror`

## Project Structure

- `tool/` — MCP tool classes (`@Component` + `@Tool`)
  - Arithmetic: `BasicCalculatorTool`, `ScientificCalculatorTool`, `FinancialCalculatorTool`
  - Graphing/Printing: `GraphingCalculatorTool`, `PrintingCalculatorTool`
  - Programmable: `ProgrammableCalculatorTool`, `VectorCalculatorTool`
  - Unit conversion: `UnitConverterTool`, `CookingConverterTool`, `MeasureReferenceTool`
  - DateTime: `DateTimeConverterTool`
- `engine/` — Expression evaluator + unit conversion registry
  - `ExpressionEvaluator` — recursive descent parser
  - `UnitCategory`, `UnitDefinition`, `UnitRegistry` — unit conversion engine
- `config/` — Dual MCP transport wiring (`McpTransportConfig`), Netty I/O selector (`NettyTransportConfig`), tool registration (`McpToolConfig`)
- `scripts/` — Python MCP integration test scripts
- `docs/` — Project documentation (architecture, MCP tools, unit conversion, datetime conversion)

## Test

- Plain JUnit 5 for tool and engine tests (no Spring context)
- `@SpringBootTest` only for `MathCalculatorApplicationTest`
