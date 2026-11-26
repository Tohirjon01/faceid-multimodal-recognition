package uz.tuit.faceid.recognition.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.tuit.faceid.recognition.config.CorrelationIdFilter;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Translates every exception into RFC 7807 ({@code application/problem+json}).
 * <p>
 * Each response carries a {@code requestId}, so when a user reports a failure
 * the exact request can be located in the logs by that identifier.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_TYPE_BASE = "https://faceid.tuit.uz/problems/";

    @ExceptionHandler(InvalidEmbeddingException.class)
    public ProblemDetail handleInvalidEmbedding(InvalidEmbeddingException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-embedding",
                "Invalid embedding", ex.getMessage());
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    public ProblemDetail handleSubjectNotFound(SubjectNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "subject-not-found",
                "Subject not found", ex.getMessage());
    }

    @ExceptionHandler(EmptyGalleryException.class)
    public ProblemDetail handleEmptyGallery(EmptyGalleryException ex) {
        return problem(HttpStatus.CONFLICT, "empty-gallery",
                "Gallery is empty", ex.getMessage());
    }

    @ExceptionHandler(ExplainNotSupportedException.class)
    public ProblemDetail handleExplainNotSupported(ExplainNotSupportedException ex) {
        return problem(HttpStatus.CONFLICT, "explain-not-supported",
                "Query plan unavailable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "validation-error",
                "Invalid request body", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "validation-error",
                "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // The full stack trace stays in the log; internal details are not
        // leaked to the caller.
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
