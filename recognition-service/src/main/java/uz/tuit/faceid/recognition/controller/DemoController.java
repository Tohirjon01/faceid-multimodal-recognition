package uz.tuit.faceid.recognition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.recognition.config.properties.MatchingProperties;
import uz.tuit.faceid.recognition.dto.request.IdentifyRequest;
import uz.tuit.faceid.recognition.dto.response.DemoStatsResponse;
import uz.tuit.faceid.recognition.dto.response.IdentifyResponse;
import uz.tuit.faceid.recognition.dto.response.SeedResponse;
import uz.tuit.faceid.recognition.entity.DemoProbe;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.DemoDataService;
import uz.tuit.faceid.recognition.service.MatchingService;

import java.util.List;

/**
 * Demo endpoints, used by the static panel.
 * <p>
 * In the real system these probes arrive from the Python inference-service;
 * until the model is wired in, the vectors are generated synthetically.
 */
@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo", description = "Synthetic probes for the demo panel")
public class DemoController {

    private static final List<String> DEMO_MODALITIES = List.of("rgb", "depth", "ir");
    private static final String DEMO_CAMERA = "cam-01";
    private static final double GENUINE_LIVENESS = 0.97;
    private static final double STRANGER_LIVENESS = 0.95;

    private final DemoDataService demoDataService;
    private final MatchingService matchingService;
    private final TemplateRepository templateRepository;
    private final MatchingProperties matchingProperties;

    public DemoController(DemoDataService demoDataService,
                          MatchingService matchingService,
                          TemplateRepository templateRepository,
                          MatchingProperties matchingProperties) {
        this.demoDataService = demoDataService;
        this.matchingService = matchingService;
        this.templateRepository = templateRepository;
        this.matchingProperties = matchingProperties;
    }

    @GetMapping("/stats")
    @Operation(summary = "Gallery size and matching settings")
    public DemoStatsResponse stats() {
        return new DemoStatsResponse(
                templateRepository.countSubjects(),
                templateRepository.countTemplates(),
                templateRepository.getBackendName(),
                matchingProperties.getSimilarityThreshold(),
                matchingProperties.getLivenessThreshold(),
                matchingProperties.getEmbeddingDimension(),
                matchingProperties.getActiveModelId());
    }

    @PostMapping("/seed")
    @Operation(summary = "Populate the gallery with demo data")
    public SeedResponse seed(@RequestParam(defaultValue = "30") int subjects,
                             @RequestParam(defaultValue = "2") int templatesPerSubject) {
        return demoDataService.seed(subjects, templatesPerSubject);
    }

    @PostMapping("/identify/known")
    @Operation(summary = "Enrolled subject", description = "GRANTED is expected")
    public IdentifyResponse identifyKnown() {
        DemoProbe probe = demoDataService.createKnownProbe();
        return matchingService.identify(buildRequest(probe, GENUINE_LIVENESS));
    }

    @PostMapping("/identify/stranger")
    @Operation(summary = "Subject not in the gallery", description = "UNKNOWN is expected")
    public IdentifyResponse identifyStranger() {
        DemoProbe probe = demoDataService.createStrangerProbe();
        return matchingService.identify(buildRequest(probe, STRANGER_LIVENESS));
    }

    @PostMapping("/identify/spoof")
    @Operation(summary = "Photo attack simulation",
            description = "SPOOF is expected even though the subject is recognised")
    public IdentifyResponse identifySpoof() {
        DemoProbe probe = demoDataService.createKnownProbe();
        return matchingService.identify(
                buildRequest(probe, demoDataService.getSpoofLivenessScore()));
    }

    @GetMapping(value = "/explain", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Query plan for a real probe")
    public String explain() {
        DemoProbe probe = demoDataService.createKnownProbe();
        return matchingService.explainSearch(probe.embedding(), null);
    }

    private IdentifyRequest buildRequest(DemoProbe probe, double livenessScore) {
        return new IdentifyRequest(
                probe.embedding(),
                null,
                livenessScore,
                DEMO_CAMERA,
                DEMO_MODALITIES,
                demoDataService.sampleExpressionProbabilities());
    }
}
