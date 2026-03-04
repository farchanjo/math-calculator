package com.archanjo.mathcalculator;

import org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebFluxAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebFluxAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.ToolCallbackConverterAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        McpServerAutoConfiguration.class,
        McpServerSseWebFluxAutoConfiguration.class,
        McpServerStreamableHttpWebFluxAutoConfiguration.class,
        ToolCallbackConverterAutoConfiguration.class
})
public class MathCalculatorApplication {
    static void main(final String... args) {
        new MathCalculatorApplication().start(args);
    }

    private void start(final String... args) {
        SpringApplication.run(MathCalculatorApplication.class, args);
    }
}
