package uz.tuit.faceid.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new person")
public record PersonCreateRequest(

        @Schema(description = "Full name", example = "Sadullayev T.")
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must not exceed 255 characters")
        String fullName,

        @Schema(description = "Role; an unrecognised value falls back to 'employee'",
                example = "employee",
                allowableValues = {"employee", "manager", "guest", "security"})
        @Size(max = 50, message = "must not exceed 50 characters")
        String role
) {
}
