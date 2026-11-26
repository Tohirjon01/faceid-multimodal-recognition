package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Result of attaching a template")
public record EnrollResponse(

        UUID templateId,

        UUID subjectId,

        int modelId,

        int embeddingDimension,

        @Schema(description = "Total number of templates now held for this subject")
        int totalTemplatesForSubject
) {
}
