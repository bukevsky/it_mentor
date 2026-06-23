package com.example.it.mentor.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MdcFilter")
class MdcFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final MdcFilter filter = new MdcFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("кладёт requestId в MDC и очищает его после запроса")
    void shouldPopulateAndClearRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(REQUEST_ID_HEADER, "req-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> requestIdInChain.set(MDC.get("requestId")));

        assertThat(requestIdInChain.get()).isEqualTo("req-12345");
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("req-12345");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("сбрасывает stale userId и генерирует requestId при его отсутствии")
    void shouldRemoveStaleUserIdAndGenerateRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> userIdInChain = new AtomicReference<>();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        MDC.put("userId", "stale-user");

        filter.doFilter(request, response, (req, res) -> {
            userIdInChain.set(MDC.get("userId"));
            requestIdInChain.set(MDC.get("requestId"));
        });

        assertThat(userIdInChain.get()).isNull();
        assertThat(requestIdInChain.get()).hasSize(8);
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo(requestIdInChain.get());
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }
}
