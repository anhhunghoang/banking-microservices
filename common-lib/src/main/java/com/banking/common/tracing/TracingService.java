package com.banking.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class TracingService {
    private final Tracer tracer;
    private final Propagator propagator;

    public String getCurrentTraceId() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null && currentSpan.context() != null) {
            return currentSpan.context().traceId();
        }
        return null;
    }

    public void runWithSpan(String spanName, String traceId, Runnable action) {
        if (tracer.currentSpan() != null) {
            // Already in a span, just join or create child
            Span span = tracer.nextSpan().name(spanName);
            try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
                action.run();
            } finally {
                span.end();
            }
            return;
        }

        Span.Builder builder;
        // If we have a traceId from the DB, we attempt to join that trace
        if (traceId != null && !traceId.isEmpty()) {
            // W3C format: 00-<traceId>-<spanId>-<flags>
            // We use a dummy spanId because we don't have the original one,
            // but we MUST have the correct traceId to link them in Jaeger.
            String traceparent = String.format("00-%s-%s-01", traceId, "0000000000000001");

            // extract() returns a Span.Builder already pointing to the parent
            builder = propagator.extract(traceparent, (carrier, key) -> {
                if (key.equals("traceparent"))
                    return carrier;
                return null;
            }).name(spanName);

            log.debug("Restored trace context for ID: {}", traceId);
        } else {
            builder = tracer.spanBuilder().name(spanName);
        }

        Span span = builder.start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            action.run();
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public <T> T callWithSpan(String spanName, String traceId, Supplier<T> action) {
        Span span = tracer.nextSpan().name(spanName);
        if (traceId != null) {
            span.tag("original_trace_id", traceId);
        }

        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            return action.get();
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
