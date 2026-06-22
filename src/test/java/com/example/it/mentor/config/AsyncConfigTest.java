package com.example.it.mentor.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    void mailExecutor_propagatesMdcContext() throws Exception {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.mailExecutor();
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

        try {
            MDC.put("requestId", "req-123");
            MDC.put("userId", "42");

            CompletableFuture<Map<String, String>> contextFuture = new CompletableFuture<>();
            executor.execute(() -> contextFuture.complete(MDC.getCopyOfContextMap()));

            assertThat(contextFuture.get(2, TimeUnit.SECONDS))
                    .containsEntry("requestId", "req-123")
                    .containsEntry("userId", "42");
        } finally {
            MDC.clear();
            taskExecutor.shutdown();
        }
    }
}
