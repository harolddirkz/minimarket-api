package com.minimarket.infrastructure.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldPreserveSafeCorrelationIdAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "mobile-sale-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> valueInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                valueInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(valueInsideChain).hasValue("mobile-sale-123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("mobile-sale-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldReplaceUnsafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "unsafe\nforged-log");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isNotEqualTo("unsafe\nforged-log")
                .matches("[0-9a-f-]{36}");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
