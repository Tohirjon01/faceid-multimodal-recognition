package uz.tuit.faceid.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Headline figures over the event window")
public record SummaryResponse(

        int totalEvents,

        @Schema(description = "Counts per decision")
        Map<String, Long> byDecision,

        @Schema(description = "Share of events that granted access, in [0..1]", example = "0.5")
        Double grantRate,

        @Schema(description = "Share of spoof attempts, in [0..1]", example = "0.25")
        Double spoofRate,

        @Schema(description = "Mean similarity across GRANTED events only")
        Double avgSimilarityWhenGranted,

        Double avgLiveness,

        Instant firstEventAt,

        Instant lastEventAt
) {
}
