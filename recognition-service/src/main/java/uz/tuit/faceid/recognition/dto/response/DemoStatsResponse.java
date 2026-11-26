package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current gallery size and matching settings")
public record DemoStatsResponse(

        int subjects,

        int templates,

        @Schema(description = "Storage backend", example = "PostgreSQL + pgvector (HNSW cosine)")
        String backend,

        double similarityThreshold,

        double livenessThreshold,

        int embeddingDimension,

        int activeModelId
) {
}
