package com.archanjo.mathcalculator.config;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.resources.LoopResources;

@Configuration
public class NettyTransportConfig {

    private static final Logger LOG = LoggerFactory.getLogger(NettyTransportConfig.class);

    private static final Map<String, String> TRANSPORTS = Map.of(
            "io.netty.channel.uring.IoUring", "io_uring",
            "io.netty.channel.epoll.Epoll", "epoll",
            "io.netty.channel.kqueue.KQueue", "kqueue"
    );

    private static final String[] TRANSPORT_ORDER = {
            "io.netty.channel.uring.IoUring",
            "io.netty.channel.epoll.Epoll",
            "io.netty.channel.kqueue.KQueue"
    };
    @Bean
    public String nettyTransportName() {
        final String transport = detectTransport();
        LOG.info("Netty I/O selector: {}", transport);
        return transport;
    }

    @Bean
    public LoopResources loopResources(final String transportName) {
        final String prefix = "calc-" + transportName;
        return LoopResources.create(prefix);
    }

    @Bean
    public NettyServerCustomizer ioSelectorCustomizer(final LoopResources loopResources) {
        return server -> server.runOn(loopResources);
    }

    static String detectTransport() {
        String result = "nio";
        for (final String className : TRANSPORT_ORDER) {
            if (isAvailable(className)) {
                result = TRANSPORTS.get(className);
                break;
            }
        }
        return result;
    }

    private static boolean isAvailable(final String className) {
        boolean result = false;
        try {
            final Class<?> clazz = Class.forName(className);
            final Method method = clazz.getMethod("isAvailable");
            result = Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // transport not available
        }
        return result;
    }
}
