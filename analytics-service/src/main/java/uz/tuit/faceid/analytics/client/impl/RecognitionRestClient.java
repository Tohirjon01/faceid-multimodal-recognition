package uz.tuit.faceid.analytics.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uz.tuit.faceid.analytics.client.RecognitionClient;
import uz.tuit.faceid.analytics.config.CorrelationIdFilter;
import uz.tuit.faceid.analytics.config.properties.AnalyticsProperties;
import uz.tuit.faceid.analytics.entity.RecognitionEventView;
import uz.tuit.faceid.analytics.exception.UpstreamUnavailableException;

import java.util.List;

/**
 * HTTP implementation.
 * <p>
 * Analytics has no database of its own — it asks the upstream service for the
 * events. That keeps data ownership intact: the audit log belongs to
 * recognition-service.
 * <p>
 * <b>Timeouts are mandatory.</b> Without them a call waits indefinitely when
 * the upstream slows down: Tomcat threads fill up and analytics stops
 * answering too, so one service's failure takes down the other. A short
 * timeout breaks that chain.
 */
@Component
public class RecognitionRestClient implements RecognitionClient {

    private static final Logger log = LoggerFactory.getLogger(RecognitionRestClient.class);

    private static final ParameterizedTypeReference<List<RecognitionEventView>> EVENT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final AnalyticsProperties properties;

    public RecognitionRestClient(RestClient.Builder builder, AnalyticsProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        this.restClient = builder
                .baseUrl(properties.getRecognitionBaseUrl())
                .requestFactory(requestFactory)
                // Propagate the correlation id so the same identifier shows up
                // in the recognition-service log and the two traces can be
                // tied together.
                .requestInterceptor((request, body, execution) -> {
                    String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (requestId != null) {
                        request.getHeaders().add(CorrelationIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public List<RecognitionEventView> fetchRecentEvents(int limit) {
        try {
            List<RecognitionEventView> events = restClient.get()
                    .uri(uri -> uri.path("/api/v1/events").queryParam("limit", limit).build())
                    .retrieve()
                    .body(EVENT_LIST);
            return events == null ? List.of() : events;
        } catch (RestClientException ex) {
            log.warn("recognition-service did not answer ({}): {}",
                    properties.getRecognitionBaseUrl(), ex.getMessage());
            throw new UpstreamUnavailableException(properties.getRecognitionBaseUrl(), ex);
        }
    }

    @Override
    public String getSourceUrl() {
        return properties.getRecognitionBaseUrl();
    }
}
