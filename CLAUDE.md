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
python3 scripts/mcp_test.py
```

## Conventions

- **Package**: `com.archanjo.mathcalculator`
- **Group**: `com.archanjo`
- **Java**: 25 (toolchain)
- **Port**: 44321
- **Transport**: SSE via WebFlux + Netty
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

## Linting

- **Checkstyle 10.23.1**: `checkstyle.xml` -- zero warnings, zero errors
- **PMD 7.22.0**: `pmd-rules.xml` -- console output
  - Excluded rules: `UnnecessaryConstructor`, `AtLeastOneConstructor`, `CommentDefaultAccessModifier`
- **Compiler**: `-Xlint:all -Xlint:-incubating -Werror`

## Project Structure

- `tool/` — MCP tool classes (`@Component` + `@Tool`)
- `engine/` — Expression evaluator (static utility)
- `config/` — Netty transport config + MCP tool registration
- `scripts/` — Python MCP integration test scripts

## Test

- Plain JUnit 5 for tool and engine tests (no Spring context)
- `@SpringBootTest` only for `MathCalculatorApplicationTest`
