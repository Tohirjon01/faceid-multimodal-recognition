package uz.tuit.faceid.analytics.service.impl;

import org.springframework.stereotype.Service;
import uz.tuit.faceid.analytics.client.RecognitionClient;
import uz.tuit.faceid.analytics.config.properties.AnalyticsProperties;
import uz.tuit.faceid.analytics.dto.response.BucketResponse;
import uz.tuit.faceid.analytics.dto.response.ReportResponse;
import uz.tuit.faceid.analytics.dto.response.SummaryResponse;
import uz.tuit.faceid.analytics.dto.response.TimeBucketResponse;
import uz.tuit.faceid.analytics.entity.RecognitionEventView;
import uz.tuit.faceid.analytics.service.AnalyticsService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String GRANTED = "GRANTED";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String SPOOF = "SPOOF";
    private static final String DENIED = "DENIED";

    private static final List<String> ALL_DECISIONS = List.of(GRANTED, UNKNOWN, SPOOF, DENIED);

    private final RecognitionClient recognitionClient;
    private final AnalyticsProperties properties;

    public AnalyticsServiceImpl(RecognitionClient recognitionClient,
                                AnalyticsProperties properties) {
        this.recognitionClient = recognitionClient;
        this.properties = properties;
    }

    @Override
    public SummaryResponse summarise(Integer window) {
        return buildSummary(fetch(window));
    }

    @Override
    public List<BucketResponse> expressionDistribution(Integer window) {
        return buildExpressionDistribution(fetch(window));
    }

    @Override
    public List<BucketResponse> cameraDistribution(Integer window) {
        return buildCameraDistribution(fetch(window));
    }

    @Override
    public List<TimeBucketResponse> timeline(Integer window) {
        return buildTimeline(fetch(window));
    }

    @Override
    public ReportResponse fullReport(Integer window) {
        // One upstream call instead of four. It lightens the load on the source
        // and guarantees that every section of the report describes the same
        // set of events.
        List<RecognitionEventView> events = fetch(window);

        return new ReportResponse(
                Instant.now(),
                recognitionClient.getSourceUrl(),
                buildSummary(events),
                buildExpressionDistribution(events),
                buildCameraDistribution(events),
                buildTimeline(events));
    }

    // ── aggregations (pure functions) ──────────────────────────

    SummaryResponse buildSummary(List<RecognitionEventView> events) {
        Map<String, Long> byDecision = new LinkedHashMap<>();
        ALL_DECISIONS.forEach(decision -> byDecision.put(decision, 0L));

        for (RecognitionEventView event : events) {
            byDecision.merge(event.decision() == null ? UNKNOWN : event.decision(), 1L, Long::sum);
        }

        int total = events.size();
        return new SummaryResponse(
                total,
                byDecision,
                share(byDecision.getOrDefault(GRANTED, 0L), total),
                share(byDecision.getOrDefault(SPOOF, 0L), total),
                average(events, event -> GRANTED.equals(event.decision())
                        ? event.matchSimilarity()
                        : null),
                average(events, RecognitionEventView::livenessScore),
                boundary(events, true),
                boundary(events, false));
    }

    List<BucketResponse> buildExpressionDistribution(List<RecognitionEventView> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        long withExpression = 0;
        for (RecognitionEventView event : events) {
            if (event.expression() == null) {
                continue;
            }
            counts.merge(event.expression(), 1L, Long::sum);
            withExpression++;
        }
        return toSortedBuckets(counts, (int) withExpression);
    }

    List<BucketResponse> buildCameraDistribution(List<RecognitionEventView> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (RecognitionEventView event : events) {
            counts.merge(event.cameraId() == null ? "unknown" : event.cameraId(), 1L, Long::sum);
        }
        return toSortedBuckets(counts, events.size());
    }

    List<TimeBucketResponse> buildTimeline(List<RecognitionEventView> events) {
        Map<Instant, long[]> buckets = new TreeMap<>();

        for (RecognitionEventView event : events) {
            if (event.occurredAt() == null) {
                continue;
            }
            Instant minute = event.occurredAt().truncatedTo(ChronoUnit.MINUTES);
            long[] counters = buckets.computeIfAbsent(minute, key -> new long[4]);
            counters[0]++;
            switch (event.decision() == null ? "" : event.decision()) {
                case GRANTED -> counters[1]++;
                case UNKNOWN -> counters[2]++;
                case SPOOF -> counters[3]++;
                default -> { /* DENIED and unknown values only add to the total */ }
            }
        }

        List<TimeBucketResponse> timeline = new ArrayList<>(buckets.size());
        buckets.forEach((minute, counters) -> timeline.add(new TimeBucketResponse(
                minute, counters[0], counters[1], counters[2], counters[3])));
        return timeline;
    }

    // ── helpers ────────────────────────────────────────────────

    private List<RecognitionEventView> fetch(Integer window) {
        int limit = window == null
                ? properties.getEventWindow()
                : Math.clamp(window, 1, properties.getMaxEventWindow());
        return recognitionClient.fetchRecentEvents(limit);
    }

    private static List<BucketResponse> toSortedBuckets(Map<String, Long> counts, int total) {
        List<BucketResponse> buckets = new ArrayList<>(counts.size());
        counts.forEach((key, count) -> buckets.add(new BucketResponse(key, count, share(count, total))));
        buckets.sort(Comparator.comparingLong(BucketResponse::count).reversed());
        return buckets;
    }

    private static Double share(long part, int total) {
        return total == 0 ? null : Math.round(part * 10_000.0 / total) / 10_000.0;
    }

    private static Double average(List<RecognitionEventView> events,
                                  Function<RecognitionEventView, Double> extractor) {
        double sum = 0.0;
        int count = 0;
        for (RecognitionEventView event : events) {
            Double value = extractor.apply(event);
            if (value != null) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? null : Math.round(sum / count * 10_000.0) / 10_000.0;
    }

    private static Instant boundary(List<RecognitionEventView> events, boolean earliest) {
        return events.stream()
                .map(RecognitionEventView::occurredAt)
                .filter(Objects::nonNull)
                .reduce((first, second) -> earliest
                        ? (first.isBefore(second) ? first : second)
                        : (first.isAfter(second) ? first : second))
                .orElse(null);
    }
}
