package uz.tuit.faceid.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One group in a distribution")
public record BucketResponse(

        @Schema(description = "Group key", example = "neutral")
        String key,

        long count,

        @Schema(description = "Share in [0..1]", example = "0.75")
        Double share
) {
}
