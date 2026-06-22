package com.example.it.mentor.websocket;

import com.example.it.mentor.security.JwtAuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketJwtChannelInterceptor")
class WebSocketJwtChannelInterceptorTest {

    @InjectMocks private WebSocketJwtChannelInterceptor interceptor;
    @Mock private JwtAuthenticationService authenticationService;
    @Mock private MessageChannel channel;

    @Test
    @DisplayName("CONNECT с Bearer JWT устанавливает Principal")
    void connectWithBearerToken_shouldSetPrincipal() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of());
        when(authenticationService.authenticate("jwt-token")).thenReturn(authentication);
        Message<byte[]> connect = connectMessage("Bearer jwt-token");

        Message<?> result = interceptor.preSend(connect, channel);

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isSameAs(authentication);
    }

    @Test
    @DisplayName("CONNECT без JWT отклоняется")
    void connectWithoutToken_shouldThrowBadCredentials() {
        Message<byte[]> connect = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(connect, channel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authorization");
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
