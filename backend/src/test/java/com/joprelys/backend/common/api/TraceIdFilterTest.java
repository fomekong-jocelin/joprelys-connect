package com.joprelys.backend.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void wellFormedClientTraceIdIsPropagated() throws Exception {
        assertThat(traceIdSeenDownstream("client-trace_42")).isEqualTo("client-trace_42");
    }

    @Test
    void forgedTraceIdsAreReplacedByAGeneratedIdentifier() throws Exception {
        for (String forged : new String[] {
                "abc\r\nFAKE LOG LINE", "x\",\"injected\":\"1", "a".repeat(65), " ", "<script>"}) {
            String seen = traceIdSeenDownstream(forged);

            assertThat(seen).isNotEqualTo(forged).matches("trc_[a-f0-9]{20}");
        }
    }

    @Test
    void missingTraceIdIsGenerated() throws Exception {
        assertThat(traceIdSeenDownstream(null)).matches("trc_[a-f0-9]{20}");
    }

    private String traceIdSeenDownstream(String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader(TraceIdFilter.TRACE_ID_HEADER, headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilterInternal(request, response,
                (req, res) -> seen.set(org.slf4j.MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)));

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(seen.get());
        assertThat(org.slf4j.MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
        return seen.get();
    }
}
