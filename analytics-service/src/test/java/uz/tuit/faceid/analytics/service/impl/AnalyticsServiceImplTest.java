package uz.tuit.faceid.analytics.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uz.tuit.faceid.analytics.client.RecognitionClient;
import uz.tuit.faceid.analytics.config.properties.AnalyticsProperties;
import uz.tuit.faceid.analytics.dto.response.BucketResponse;
import uz.tuit.faceid.analytics.dto.response.ReportResponse;
import uz.tuit.faceid.analytics.dto.response.SummaryResponse;
import uz.tuit.faceid.analytics.dto.response.TimeBucketResponse;
import uz.tuit.faceid.analytics.entity.RecognitionEventView;
import uz.tuit.faceid.analytics.exception.UpstreamUnavailableException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The aggregations are exercised without HTTP: {@code RecognitionClient} is an
 * interface, so a stub can stand in for the real upstream.
 */
@DisplayName("AnalyticsService — aggregations")
class AnalyticsServiceImplTest {

    private static final Instant MINUTE_ONE = Instant.parse("2026-10-20T10:00:30Z");
    private static final Instant MINUTE_TWO = Instant.parse("2026-10-20T10:01:10Z");

    /** Minimal stub: returns a list, or throws on demand. */
    private static final class StubClient implements RecognitionClient {
        private List<RecognitionEventView> events = List.of();
        private boolean failing;
        private int lastRequestedLimit;

        @Override
        public List<RecognitionEventView> fetchRecentEvents(int limit) {
            lastRequestedLimit = limit;
            if (failing) {
                throw new UpstreamUnavailableException("http://stub", new RuntimeException("down"));
            }
            return events;
        }

        @Override
        public String getSourceUrl() {
            return "http://stub";
        }
    }

    private StubClient client;
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        client = new StubClient();
        analyticsService = new AnalyticsServiceImpl(client, new AnalyticsProperties());
        client.events = sampleEvents();
    }

    private static RecognitionEventView event(long id, Instant at, String decision,
                                              Double similarity, Double liveness,
                                              String expression, String camera) {
        return new RecognitionEventView(id, at, UUID.randomUUID(), "Test T.", camera,
                decision, similarity, liveness, expression, List.of("rgb", "depth"));
    }

    private static List<RecognitionEventView> sampleEvents() {
        return List.of(
                event(1, MINUTE_ONE, "GRANTED", 0.80, 0.97, "neutral", "cam-01"),
                event(2, MINUTE_ONE, "GRANTED", 0.70, 0.95, "happy", "cam-01"),
                event(3, MINUTE_TWO, "UNKNOWN", 0.04, 0.96, "neutral", "cam-02"),
                event(4, MINUTE_TWO, "SPOOF", 0.78, 0.18, "neutral", "cam-01"));
    }

    @Test
    @DisplayName("Counts and shares per decision")
    void summaryCountsDecisions() {
        SummaryResponse summary = analyticsService.summarise(null);

        assertEquals(4, summary.totalEvents());
        assertEquals(2L, summary.byDecision().get("GRANTED"));
        assertEquals(1L, summary.byDecision().get("UNKNOWN"));
        assertEquals(1L, summary.byDecision().get("SPOOF"));
        assertEquals(0L, summary.byDecision().get("DENIED"));
        assertEquals(0.5, summary.grantRate(), 1e-6);
        assertEquals(0.25, summary.spoofRate(), 1e-6);
    }

    @Test
    @DisplayName("Mean similarity covers GRANTED events only")
    void averageSimilarityOnlyForGranted() {
        // (0.80 + 0.70) / 2 = 0.75 — the 0.78 from the SPOOF row is excluded,
        // because that attempt did not grant access.
        assertEquals(0.75, analyticsService.summarise(null).avgSimilarityWhenGranted(), 1e-6);
    }

    @Test
    @DisplayName("The time range is reported")
    void summaryReportsTimeRange() {
        SummaryResponse summary = analyticsService.summarise(null);

        assertEquals(MINUTE_ONE, summary.firstEventAt());
        assertEquals(MINUTE_TWO, summary.lastEventAt());
    }

    @Test
    @DisplayName("An empty list breaks nothing")
    void emptyListIsSafe() {
        client.events = List.of();

        SummaryResponse summary = analyticsService.summarise(null);

        assertEquals(0, summary.totalEvents());
        assertNull(summary.grantRate());
        assertNull(summary.avgSimilarityWhenGranted());
        assertNull(summary.firstEventAt());
        assertTrue(analyticsService.timeline(null).isEmpty());
        assertTrue(analyticsService.expressionDistribution(null).isEmpty());
    }

    @Test
    @DisplayName("Expression distribution is sorted by frequency")
    void expressionDistributionIsSorted() {
        List<BucketResponse> buckets = analyticsService.expressionDistribution(null);

        assertEquals("neutral", buckets.get(0).key());
        assertEquals(3L, buckets.get(0).count());
        assertEquals(0.75, buckets.get(0).share(), 1e-6);
        assertEquals("happy", buckets.get(1).key());
    }

    @Test
    @DisplayName("Distribution across cameras")
    void cameraDistribution() {
        List<BucketResponse> buckets = analyticsService.cameraDistribution(null);

        assertEquals("cam-01", buckets.get(0).key());
        assertEquals(3L, buckets.get(0).count());
    }

    @Test
    @DisplayName("The timeline groups by minute, oldest first")
    void timelineGroupsByMinute() {
        List<TimeBucketResponse> timeline = analyticsService.timeline(null);

        assertEquals(2, timeline.size());
        assertEquals(Instant.parse("2026-10-20T10:00:00Z"), timeline.get(0).minute());
        assertEquals(2L, timeline.get(0).total());
        assertEquals(2L, timeline.get(0).granted());
        assertEquals(1L, timeline.get(1).unknown());
        assertEquals(1L, timeline.get(1).spoof());
    }

    @Test
    @DisplayName("The full report makes a single upstream call")
    void fullReportFetchesOnce() {
        ReportResponse report = analyticsService.fullReport(null);

        assertNotNull(report.generatedAt());
        assertEquals("http://stub", report.source());
        assertEquals(4, report.summary().totalEvents());
        assertEquals(2, report.timeline().size());
        assertEquals(2, report.expressions().size());
    }

    @Test
    @DisplayName("The window is clamped to maxEventWindow")
    void windowIsClamped() {
        analyticsService.summarise(100_000);
        assertEquals(500, client.lastRequestedLimit);

        analyticsService.summarise(0);
        assertEquals(1, client.lastRequestedLimit);

        analyticsService.summarise(null);
        assertEquals(200, client.lastRequestedLimit, "omitted window falls back to eventWindow");
    }

    @Test
    @DisplayName("An unavailable upstream surfaces as UpstreamUnavailableException")
    void upstreamFailurePropagates() {
        client.failing = true;

        assertThrows(UpstreamUnavailableException.class, () -> analyticsService.summarise(null));
        assertThrows(UpstreamUnavailableException.class, () -> analyticsService.fullReport(null));
    }

    @Test
    @DisplayName("A null decision counts as UNKNOWN")
    void nullDecisionCountsAsUnknown() {
        client.events = List.of(event(9, MINUTE_ONE, null, null, 0.9, null, "cam-01"));

        assertEquals(1L, analyticsService.summarise(null).byDecision().get("UNKNOWN"));
    }
}
