package uz.tuit.faceid.recognition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.recognition.dto.request.EnrollRequest;
import uz.tuit.faceid.recognition.dto.request.IdentifyRequest;
import uz.tuit.faceid.recognition.dto.response.EnrollResponse;
import uz.tuit.faceid.recognition.dto.response.IdentifyResponse;
import uz.tuit.faceid.recognition.service.MatchingService;

/**
 * The primary contract — these endpoints are called by the Python
 * inference-service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Recognition", description = "Biometric matching and enrollment")
public class RecognitionController {

    private final MatchingService matchingService;

    public RecognitionController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/identify")
    @Operation(summary = "Identify a subject",
            description = "Finds the closest enrolled subject for an embedding and decides")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision produced"),
            @ApiResponse(responseCode = "400", description = "Invalid embedding")
    })
    public IdentifyResponse identify(@Valid @RequestBody IdentifyRequest request) {
        return matchingService.identify(request);
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a template",
            description = "Adds a new biometric template to an existing subject")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Template stored"),
            @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    public EnrollResponse enroll(@Valid @RequestBody EnrollRequest request) {
        return matchingService.enroll(request);
    }

    @PostMapping(value = "/explain", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Query plan",
            description = "Shows that the HNSW index is used; PostgreSQL backend only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EXPLAIN ANALYZE output"),
            @ApiResponse(responseCode = "409", description = "No plan on the in-memory backend")
    })
    public String explain(@Valid @RequestBody IdentifyRequest request) {
        return matchingService.explainSearch(request.embedding(), request.modelId());
    }
}
