package uz.tuit.faceid.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Full report — every figure in a single response")
public record ReportResponse(

        Instant generatedAt,

        @Schema(description = "Address of the service the data came from")
        String source,

        SummaryResponse summary,

        List<BucketResponse> expressions,

        List<BucketResponse> cameras,

        List<TimeBucketResponse> timeline
) {
}
