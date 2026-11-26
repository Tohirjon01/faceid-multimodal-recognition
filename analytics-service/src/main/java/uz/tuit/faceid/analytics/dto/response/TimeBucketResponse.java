package uz.tuit.faceid.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "One minute on the timeline")
public record TimeBucketResponse(

        @Schema(description = "Start of the minute")
        Instant minute,

        long total,

        long granted,

        long unknown,

        long spoof
) {
}
