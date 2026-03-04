package com.archanjo.mathcalculator.config;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * Dual MCP transport configuration — serves both SSE and Streamable HTTP
 * on the same Netty port so legacy and modern clients can connect.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>SSE: {@code GET /sse} + {@code POST /mcp/message}</li>
 *   <li>Streamable HTTP: {@code POST /mcp}</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(McpServerProperties.class)
public class McpTransportConfig {

    @Bean
    McpJsonMapper mcpJsonMapper() {
        return new JacksonMcpJsonMapper(new ObjectMapper());
    }

    @Bean
    WebFluxSseServerTransportProvider sseTransport(final McpJsonMapper json) {
        return WebFluxSseServerTransportProvider.builder()
                .jsonMapper(json)
                .sseEndpoint("/sse")
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    WebFluxStreamableServerTransportProvider streamableTransport(
            final McpJsonMapper json) {
        return WebFluxStreamableServerTransportProvider.builder()
                .jsonMapper(json)
                .messageEndpoint("/mcp")
                .build();
    }

    @Bean
    RouterFunction<?> sseRouterFunction(
            final WebFluxSseServerTransportProvider sseTransport) {
        return sseTransport.getRouterFunction();
    }

    @Bean
    RouterFunction<?> streamableRouterFunction(
            final WebFluxStreamableServerTransportProvider transport) {
        return transport.getRouterFunction();
    }

    @Bean
    List<SyncToolSpecification> syncToolSpecs(final ToolCallbackProvider provider) {
        return McpToolUtils.toSyncToolSpecification(
                Arrays.asList(provider.getToolCallbacks()));
    }

    @Bean
    McpSyncServer sseMcpServer(
            final WebFluxSseServerTransportProvider sseTransport,
            final List<SyncToolSpecification> tools,
            final McpServerProperties props) {
        return McpServer.sync(sseTransport)
                .serverInfo(props.getName(), props.getVersion())
                .capabilities(buildCapabilities())
                .tools(tools)
                .build();
    }

    @Bean
    McpSyncServer streamableMcpServer(
            final WebFluxStreamableServerTransportProvider transport,
            final List<SyncToolSpecification> tools,
            final McpServerProperties props) {
        return McpServer.sync(transport)
                .serverInfo(props.getName(), props.getVersion())
                .capabilities(buildCapabilities())
                .tools(tools)
                .build();
    }

    private static ServerCapabilities buildCapabilities() {
        return ServerCapabilities.builder().tools(true).build();
    }
}
