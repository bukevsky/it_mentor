package com.example.it.mentor.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketSessionRegistry")
class WebSocketSessionRegistryTest {

    private final WebSocketSessionRegistry registry = new WebSocketSessionRegistry();

    @Test
    @DisplayName("пользователь online после первой регистрации")
    void firstSession_shouldMarkUserOnline() {
        boolean first = registry.register(10L, "session-1");

        assertThat(first).isTrue();
        assertThat(registry.isOnline(10L)).isTrue();
    }

    @Test
    @DisplayName("закрытие одной из двух сессий не переводит пользователя offline")
    void oneOfTwoSessionsClosed_shouldRemainOnline() {
        registry.register(10L, "session-1");
        registry.register(10L, "session-2");

        SessionDisconnectResult result = registry.unregister("session-1");

        assertThat(result.lastSession()).isFalse();
        assertThat(registry.isOnline(10L)).isTrue();
    }

    @Test
    @DisplayName("закрытие последней сессии переводит пользователя offline")
    void lastSessionClosed_shouldMarkUserOffline() {
        registry.register(10L, "session-1");

        SessionDisconnectResult result = registry.unregister("session-1");

        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.lastSession()).isTrue();
        assertThat(registry.isOnline(10L)).isFalse();
    }

    @Test
    @DisplayName("одновременные подключения публикуют online только один раз")
    void concurrentSessions_shouldHaveExactlyOneFirstSession() throws Exception {
        int sessionCount = 20;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(sessionCount)) {
            List<Callable<Boolean>> registrations = IntStream.range(0, sessionCount)
                    .mapToObj(index -> (Callable<Boolean>) () -> {
                        start.await();
                        return registry.register(10L, "session-" + index);
                    })
                    .toList();
            List<Future<Boolean>> futures = registrations.stream()
                    .map(executor::submit)
                    .toList();

            start.countDown();

            long firstSessionCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    firstSessionCount++;
                }
            }
            assertThat(firstSessionCount).isOne();
            assertThat(registry.isOnline(10L)).isTrue();
        }
    }
}
