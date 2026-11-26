package uz.tuit.faceid.analytics.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.tuit.faceid.analytics.config.CorrelationIdFilter;

import java.net.URI;
import java.time.Instant;

/** Translates every exception into RFC 7807 ({@code application/problem+json}). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_TYPE_BASE = "https://faceid.tuit.uz/problems/";

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ProblemDetail handleUpstreamDown(UpstreamUnavailableException ex) {
        log.warn("Upstream unavailable: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "upstream-unavailable",
                "Upstream service unavailable", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "validation-error",
                "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal error", ex.getClass().getSimpleName());
    }

    private static ProblemDetail problem(HttpStatusCode status, String code,
                                         String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + code));
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());

        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }
}
