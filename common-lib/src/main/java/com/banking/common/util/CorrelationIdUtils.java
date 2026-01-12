package com.banking.common.util;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

@UtilityClass
public class CorrelationIdUtils {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    public static void setCorrelationId(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static String getCorrelationId() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
