package uz.tuit.faceid.recognition.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Attaches a correlation identifier to every request.
 * <p>
 * In a microservice system a single user action leaves traces in several
 * service logs. Without a shared identifier, tying those lines back together
 * is close to impossible: "which log line belongs to which request?" has no
 * answer.
 * <p>
 * An incoming {@code X-Request-Id} is preserved, so an identifier issued by
 * the gateway or by a calling service travels through the whole chain.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // MDC lives in a ThreadLocal; without this cleanup the next request
            // served by the same thread would inherit the stale identifier.
            MDC.remove(MDC_KEY);
        }
    }
}
