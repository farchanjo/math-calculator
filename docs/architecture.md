# Architecture

## System Overview

```mermaid
graph TD
    A[MCP Client<br/>Claude Desktop / Inspector] -->|SSE /sse| B[Netty Server]
    B --> N{Transport Selector}
    N -->|Linux| N1[io_uring<br/>preferred]
    N -->|Linux fallback| N2[epoll]
    N -->|macOS| N3[kqueue]
    N -->|fallback| N4[NIO]
    B --> C[MCP Server Auto-Config]
    C --> D[McpToolConfig<br/>MethodToolCallbackProvider]
    D --> E[BasicCalculatorTool<br/>BigDecimal]
    D --> F[ScientificCalculatorTool<br/>StrictMath]
    D --> G[VectorCalculatorTool<br/>SIMD]
    D --> H[GraphingCalculatorTool<br/>ExpressionEvaluator]
    D --> I[FinancialCalculatorTool<br/>BigDecimal]
    D --> J[PrintingCalculatorTool<br/>tape/audit]
    D --> K[ProgrammableCalculatorTool<br/>ExpressionEvaluator]

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

## SSE Flow

```mermaid
sequenceDiagram
    participant Client as MCP Client
    participant Server as Netty/WebFlux
    participant MCP as MCP Server
    participant Tool as Calculator Tool

    Client->>Server: GET /sse (SSE connect)
    Server-->>Client: SSE stream opened
    Client->>Server: POST /mcp/message (tool call)
    Server->>MCP: Route to tool
    MCP->>Tool: Invoke @Tool method
    Tool-->>MCP: Return result
    MCP-->>Server: MCP response
    Server-->>Client: SSE event (result)
```

## Package Structure

```mermaid
graph TB
    subgraph "com.archanjo.mathcalculator"
        APP[MathCalculatorApplication]
        subgraph config
            NTC[NettyTransportConfig]
            MTC[McpToolConfig]
        end
        subgraph engine
            EE[ExpressionEvaluator]
        end
        subgraph tool
            BCT[BasicCalculatorTool]
            SCT[ScientificCalculatorTool]
            VCT[VectorCalculatorTool]
            GCT[GraphingCalculatorTool]
            FCT[FinancialCalculatorTool]
            PCT[PrintingCalculatorTool]
            PRCT[ProgrammableCalculatorTool]
        end
    end

    MTC --> BCT
    MTC --> SCT
    MTC --> VCT
    MTC --> GCT
    MTC --> FCT
    MTC --> PCT
    MTC --> PRCT
    GCT --> EE
    PRCT --> EE
```
