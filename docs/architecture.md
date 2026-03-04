# Architecture

## System Overview

```mermaid
graph TD
    A1[MCP Client<br/>Claude Code / OpenCode / modern] -->|HTTP POST /mcp<br/>Streamable HTTP| B[Netty Server]
    A2[MCP Client<br/>Claude Desktop / Inspector / legacy] -->|GET /sse + POST /mcp/message<br/>SSE| B
    B --> N{Transport Selector}
    N -->|Linux| N1[io_uring<br/>preferred]
    N -->|Linux fallback| N2[epoll]
    N -->|macOS| N3[kqueue]
    N -->|fallback| N4[NIO]
    B --> C[McpTransportConfig<br/>Dual Transport]
    C --> D[McpToolConfig<br/>MethodToolCallbackProvider]
    D --> E[BasicCalculatorTool<br/>BigDecimal]
    D --> F[ScientificCalculatorTool<br/>StrictMath + lookup tables]
    D --> G[VectorCalculatorTool<br/>SIMD]
    D --> H[GraphingCalculatorTool<br/>ExpressionEvaluator + BigDecimal steps]
    D --> I[FinancialCalculatorTool<br/>BigDecimal]
    D --> J[PrintingCalculatorTool<br/>tape/audit]
    D --> K[ProgrammableCalculatorTool<br/>ExpressionEvaluator]
    D --> L[UnitConverterTool<br/>UnitRegistry]
    D --> M[CookingConverterTool<br/>UnitRegistry + gas mark]
    D --> O[MeasureReferenceTool<br/>UnitRegistry lookup]
    D --> P[DateTimeConverterTool<br/>java.time]
    D --> NCT[NetworkCalculatorTool<br/>IPv4/IPv6]
    D --> AET[AnalogElectronicsTool<br/>Ohm's law + RLC]
    D --> DET[DigitalElectronicsTool<br/>base conversion + 555]
    D --> CLT[CalculusTool<br/>derivatives + integrals]

    style N1 fill:#2d6,stroke:#333
    style N2 fill:#5a5,stroke:#333
    style N3 fill:#5a5,stroke:#333
    style N4 fill:#888,stroke:#333
```

## Concurrency Model

```mermaid
graph LR
    subgraph "Java 25 Concurrency"
        VT[Virtual Threads<br/>spring.threads.virtual.enabled=true]
    end
    VT --> |"each MCP request<br/>runs on virtual thread"| TOOL[Calculator Tools]
```

## Transport Selection

The `NettyTransportConfig` selects the best available Netty transport at startup using reflection:

```mermaid
flowchart TD
    START[Application Start] --> CHECK_URING{io_uring<br/>available?}
    CHECK_URING -->|Yes| USE_URING[Use io_uring<br/>Best Linux performance]
    CHECK_URING -->|No| CHECK_EPOLL{epoll<br/>available?}
    CHECK_EPOLL -->|Yes| USE_EPOLL[Use epoll<br/>Good Linux performance]
    CHECK_EPOLL -->|No| CHECK_KQUEUE{kqueue<br/>available?}
    CHECK_KQUEUE -->|Yes| USE_KQUEUE[Use kqueue<br/>macOS native]
    CHECK_KQUEUE -->|No| USE_NIO[Use NIO<br/>Java built-in fallback]
```

## SIMD Vector Operations

The `VectorCalculatorTool` uses the Java 25 Vector API (`jdk.incubator.vector`) for hardware-accelerated batch operations:

```mermaid
flowchart LR
    INPUT[Input Array] --> SIMD_LOOP[SIMD Loop<br/>Process N elements/cycle]
    SIMD_LOOP --> TAIL[Tail Loop<br/>Remaining elements]
    TAIL --> RESULT[Result]

    subgraph "Hardware Selection"
        SSE[SSE 128-bit<br/>2 doubles]
        AVX2[AVX2 256-bit<br/>4 doubles]
        AVX512[AVX-512 512-bit<br/>8 doubles]
    end

    SPECIES[SPECIES_PREFERRED] --> SSE
    SPECIES --> AVX2
    SPECIES --> AVX512
```

## Expression Engine

The `ExpressionEvaluator` is a recursive descent parser supporting:

```mermaid
flowchart TD
    EXPR["Expression<br/>term ((+/-) term)*"] --> TERM["Term<br/>power ((*/ / /%) power)*"]
    TERM --> POWER["Power<br/>unary (^ power)?<br/>right-associative"]
    POWER --> UNARY["Unary<br/>- unary / primary"]
    UNARY --> PRIMARY[Primary]
    PRIMARY --> NUM["Number<br/>decimal / scientific"]
    PRIMARY --> VAR["Variable<br/>lookup in map"]
    PRIMARY --> FUNC["Function Call<br/>sin, cos, sqrt, ..."]
    PRIMARY --> PAREN["Parenthesized<br/>expression"]
```

## Unit Conversion Engine

The `UnitRegistry` is a static utility backed by `UnitCategory` (enum, 21 categories) and `UnitDefinition` (record, code + name + category + toBaseFactor):

```mermaid
flowchart TD
    INPUT[Convert value, from, to] --> CHECK{Same category?}
    CHECK -->|No| ERROR[Error: cross-category]
    CHECK -->|Yes| TYPE{Category type?}
    TYPE -->|Linear| LINEAR["result = value * from.toBaseFactor / to.toBaseFactor"]
    TYPE -->|Temperature| TEMP["value -> toCelsius() -> fromCelsius()"]
    TYPE -->|Gas Mark| GAS["Lookup table (marks 1-10)"]
    LINEAR --> OUTPUT[BigDecimal result<br/>DECIMAL128, 34 digits]
    TEMP --> OUTPUT
    GAS --> OUTPUT
```

## MCP Transport Flows

### Streamable HTTP (POST /mcp)

Used by Claude Code, OpenCode, and other modern MCP clients.

```mermaid
sequenceDiagram
    participant Client as MCP Client
    participant Server as Netty/WebFlux
    participant MCP as MCP Server
    participant Tool as Calculator Tool

    Client->>Server: POST /mcp (initialize)
    Server-->>Client: 200 OK + mcp-session-id header
    Client->>Server: POST /mcp (tools/call) + mcp-session-id
    Server->>MCP: Route to tool
    MCP->>Tool: Invoke @Tool method
    Tool-->>MCP: Return result
    MCP-->>Server: MCP response
    Server-->>Client: 200 JSON or 200 text/event-stream (streaming)
```

### SSE (GET /sse + POST /mcp/message)

Used by Claude Desktop, MCP Inspector, and legacy MCP clients.

```mermaid
sequenceDiagram
    participant Client as MCP Client
    participant Server as Netty/WebFlux
    participant MCP as MCP Server
    participant Tool as Calculator Tool

    Client->>Server: GET /sse
    Server-->>Client: SSE endpoint event (message URL)
    Client->>Server: POST /mcp/message (initialize)
    Server-->>Client: 200 OK + session
    Client->>Server: POST /mcp/message (tools/call)
    Server->>MCP: Route to tool
    MCP->>Tool: Invoke @Tool method
    Tool-->>MCP: Return result
    MCP-->>Server: MCP response
    Server-->>Client: 200 JSON-RPC response
```

## Package Structure

```mermaid
graph TB
    subgraph "com.archanjo.mathcalculator"
        APP[MathCalculatorApplication]
        subgraph config
            NTC[NettyTransportConfig]
            MTPC[McpTransportConfig]
            MTC[McpToolConfig]
        end
        subgraph engine
            EE[ExpressionEvaluator]
            UC[UnitCategory]
            UD[UnitDefinition]
            UR[UnitRegistry]
        end
        subgraph tool
            BCT[BasicCalculatorTool]
            SCT[ScientificCalculatorTool]
            VCT[VectorCalculatorTool]
            GCT[GraphingCalculatorTool]
            FCT[FinancialCalculatorTool]
            PCT[PrintingCalculatorTool]
            PRCT[ProgrammableCalculatorTool]
            UCT[UnitConverterTool]
            CCT[CookingConverterTool]
            MRT[MeasureReferenceTool]
            DTCT[DateTimeConverterTool]
            NCT2[NetworkCalculatorTool]
            AET2[AnalogElectronicsTool]
            DET2[DigitalElectronicsTool]
            CLT2[CalculusTool]
        end
    end

    MTC --> BCT
    MTC --> SCT
    MTC --> VCT
    MTC --> GCT
    MTC --> FCT
    MTC --> PCT
    MTC --> PRCT
    MTC --> UCT
    MTC --> CCT
    MTC --> MRT
    MTC --> DTCT
    MTC --> NCT2
    MTC --> AET2
    MTC --> DET2
    MTC --> CLT2
    GCT --> EE
    PRCT --> EE
    UCT --> UR
    CCT --> UR
    MRT --> UR
    NCT2 --> UR
    CLT2 --> EE
    UR --> UC
    UR --> UD
```
