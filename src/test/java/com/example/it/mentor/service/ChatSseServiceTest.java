package com.example.it.mentor.service;

import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.sse.PresencePayload;
import com.example.it.mentor.dto.sse.TypingPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("ChatSseService")
class ChatSseServiceTest {

    private ChatSseService service;

    @BeforeEach
    void setUp() {
        service = new ChatSseService();
    }

    @Test
    @DisplayName("subscribe регистрирует пользователя как онлайн")
    void subscribe_withOnDisconnect_shouldMarkUserOnline() {
        service.subscribe(1L, () -> {});

        assertThat(service.isOnline(1L)).isTrue();
    }

    @Test
    @DisplayName("subscribe вызывает onDisconnect при вызове completionCallback")
    void subscribe_withOnDisconnect_shouldCallCallbackOnCompletion() {
        AtomicBoolean called = new AtomicBoolean(false);

        SseEmitter emitter = service.subscribe(1L, () -> called.set(true));
        triggerCompletionCallback(emitter);

        assertThat(called.get()).isTrue();
    }

    @Test
    @DisplayName("subscribe с null onDisconnect не выбрасывает исключение при завершении")
    void subscribe_withNullOnDisconnect_shouldNotThrowOnCompletion() {
        SseEmitter emitter = service.subscribe(1L, null);

        assertThatNoException().isThrownBy(() -> triggerCompletionCallback(emitter));
    }

    @Test
    @DisplayName("completionCallback удаляет пользователя из онлайн")
    void subscribe_afterCompletion_shouldMarkUserOffline() {
        SseEmitter emitter = service.subscribe(1L, () -> {});

        triggerCompletionCallback(emitter);

        assertThat(service.isOnline(1L)).isFalse();
    }

    @Test
    @DisplayName("isOnline возвращает true для подключённого пользователя")
    void isOnline_subscribedUser_shouldReturnTrue() {
        service.subscribe(42L, () -> {});

        assertThat(service.isOnline(42L)).isTrue();
    }

    @Test
    @DisplayName("isOnline возвращает false для неподключённого пользователя")
    void isOnline_unsubscribedUser_shouldReturnFalse() {
        assertThat(service.isOnline(99L)).isFalse();
    }

    @Test
    @DisplayName("pushTyping не выбрасывает исключение если пользователь не онлайн")
    void pushTyping_userOffline_shouldNotThrow() {
        TypingPayload payload = new TypingPayload(1L, 7L, true);

        assertThatNoException().isThrownBy(() -> service.pushTyping(99L, payload));
    }

    @Test
    @DisplayName("pushTyping не выбрасывает исключение если пользователь онлайн")
    void pushTyping_userOnline_shouldNotThrow() {
        service.subscribe(5L, () -> {});
        TypingPayload payload = new TypingPayload(1L, 7L, true);

        assertThatNoException().isThrownBy(() -> service.pushTyping(5L, payload));
    }

    @Test
    @DisplayName("pushPresenceChanged не выбрасывает исключение для нескольких получателей")
    void pushPresenceChanged_multipleRecipients_shouldNotThrow() {
        service.subscribe(1L, () -> {});
        service.subscribe(2L, () -> {});
        PresencePayload payload = new PresencePayload(7L, "online", null);

        assertThatNoException().isThrownBy(() ->
                service.pushPresenceChanged(Set.of(1L, 2L, 99L), payload));
    }

    @Test
    @DisplayName("pushMessageCreated не выбрасывает исключение если пользователи не онлайн")
    void pushMessageCreated_usersOffline_shouldNotThrow() {
        ChatMessageResponse msg = new ChatMessageResponse(1L, 1L, 1L, "hello", null, null);

        assertThatNoException().isThrownBy(() ->
                service.pushMessageCreated(10L, 20L, msg));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void triggerCompletionCallback(SseEmitter emitter) {
        // В Spring Framework 7 completionCallback — DefaultCallback implements Runnable,
        // хранит список delegates. Вызов run() имитирует завершение соединения фреймворком.
        Runnable callback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
        if (callback != null) callback.run();
    }
}
