# Math Calculator — Project Instructions

## Build & Run

```bash
./gradlew build        # compile + test + lint
./gradlew bootRun      # start on port 44321
./gradlew test         # tests only
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
- `BigDecimal` for basic/financial precision
- `StrictMath` for scientific determinism
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

## Test

- Plain JUnit 5 for tool and engine tests (no Spring context)
- `@SpringBootTest` only for `MathCalculatorApplicationTest`
