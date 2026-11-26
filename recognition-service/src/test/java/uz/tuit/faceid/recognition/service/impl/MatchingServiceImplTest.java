package uz.tuit.faceid.recognition.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uz.tuit.faceid.recognition.config.properties.MatchingProperties;
import uz.tuit.faceid.recognition.dto.request.EnrollRequest;
import uz.tuit.faceid.recognition.dto.request.IdentifyRequest;
import uz.tuit.faceid.recognition.dto.response.EnrollResponse;
import uz.tuit.faceid.recognition.dto.response.IdentifyResponse;
import uz.tuit.faceid.recognition.entity.DemoProbe;
import uz.tuit.faceid.recognition.entity.Subject;
import uz.tuit.faceid.recognition.entity.enums.Decision;
import uz.tuit.faceid.recognition.exception.ExplainNotSupportedException;
import uz.tuit.faceid.recognition.exception.InvalidEmbeddingException;
import uz.tuit.faceid.recognition.exception.SubjectNotFoundException;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.DemoDataService;
import uz.tuit.faceid.recognition.service.MatchingService;
import uz.tuit.faceid.recognition.util.VectorUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three main paths through the decision flow: GRANTED, UNKNOWN and SPOOF.
 * <p>
 * {@code WebEnvironment.NONE} keeps Tomcat out of the picture — the test
 * exercises the service layer only and runs faster for it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = "faceid.demo.auto-seed=false")
@DisplayName("MatchingService — recognition and decision flow")
class MatchingServiceImplTest {

    private static final List<String> MODALITIES = List.of("rgb", "depth", "ir");
    private static final String TEST_CAMERA = "cam-test";

    @Autowired
    MatchingService matchingService;

    @Autowired
    DemoDataService demoDataService;

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    MatchingProperties matchingProperties;

    @BeforeEach
    void seedGallery() {
        if (templateRepository.countTemplates() == 0) {
            demoDataService.seed(30, 2);
        }
    }

    private IdentifyRequest request(float[] embedding, Double liveness) {
        return new IdentifyRequest(embedding, null, liveness, TEST_CAMERA,
                MODALITIES, demoDataService.sampleExpressionProbabilities());
    }

    @Test
    @DisplayName("Seeding creates exactly two templates per subject")
    void seedCreatesTwoTemplatesPerSubject() {
        // The Spring context is shared across tests and enrollThenIdentify adds
        // another subject, so the structure is asserted rather than exact counts.
        assertTrue(templateRepository.countSubjects() >= 30);
        assertTrue(templateRepository.countTemplates() >= 60);

        long withTwoTemplates = templateRepository.findAllSubjects().stream()
                .filter(subject -> templateRepository.countTemplatesBySubject(subject.id()) == 2)
                .count();
        assertTrue(withTwoTemplates >= 30, "subjects holding two templates: " + withTwoTemplates);
    }

    @Test
    @DisplayName("Enrolled subject is GRANTED, similarity clears the threshold")
    void knownSubjectIsGranted() {
        DemoProbe probe = demoDataService.createKnownProbe();

        IdentifyResponse response = matchingService.identify(request(probe.embedding(), 0.97));

        assertEquals(Decision.GRANTED, response.decision(), response.note());
        assertEquals(probe.expectedSubjectId(), response.subjectId());
        assertNotNull(response.matchSimilarity());
        assertTrue(response.matchSimilarity() >= matchingProperties.getSimilarityThreshold());
        assertEquals(matchingProperties.getTopK(), response.candidates().size());
        assertEquals(1, response.candidates().get(0).rank());
        assertTrue(response.candidates().get(0).aboveThreshold());
    }

    @Test
    @DisplayName("The top candidate is clearly separated from the runner-up")
    void topCandidateIsClearlySeparated() {
        IdentifyResponse response = matchingService.identify(
                request(demoDataService.createKnownProbe().embedding(), 0.97));

        double first = response.candidates().get(0).similarity();
        double second = response.candidates().get(1).similarity();

        assertTrue(first - second > 0.3,
                "insufficient separation: %.3f vs %.3f".formatted(first, second));
        assertFalse(response.candidates().get(1).aboveThreshold());
    }

    @Test
    @DisplayName("Someone not in the gallery is UNKNOWN with no subject attached")
    void strangerIsUnknown() {
        IdentifyResponse response = matchingService.identify(
                request(demoDataService.createStrangerProbe().embedding(), 0.95));

        assertEquals(Decision.UNKNOWN, response.decision(), response.note());
        assertNull(response.subjectId());
        assertTrue(response.matchSimilarity() < matchingProperties.getSimilarityThreshold());
    }

    @Test
    @DisplayName("Low liveness is SPOOF even when the subject is recognised")
    void lowLivenessIsSpoofEvenForKnownSubject() {
        IdentifyResponse response = matchingService.identify(request(
                demoDataService.createKnownProbe().embedding(),
                demoDataService.getSpoofLivenessScore()));

        assertEquals(Decision.SPOOF, response.decision(), response.note());
        // Access is refused even though the face matched — liveness comes first.
        assertNull(response.subjectId());
        assertTrue(response.matchSimilarity() >= matchingProperties.getSimilarityThreshold());
    }

    @Test
    @DisplayName("A missing liveness score skips the spoof check")
    void nullLivenessSkipsSpoofCheck() {
        IdentifyResponse response = matchingService.identify(
                request(demoDataService.createKnownProbe().embedding(), null));

        assertEquals(Decision.GRANTED, response.decision(), response.note());
    }

    @Test
    @DisplayName("Wrong embedding dimension raises InvalidEmbeddingException")
    void wrongEmbeddingDimensionIsRejected() {
        IdentifyRequest bad = request(new float[]{0.1f, 0.2f}, 0.9);

        InvalidEmbeddingException exception = assertThrows(InvalidEmbeddingException.class,
                () -> matchingService.identify(bad));
        assertTrue(exception.getMessage().contains("512"), exception.getMessage());
    }

    @Test
    @DisplayName("A null embedding gives a clear error too")
    void nullEmbeddingIsRejected() {
        assertThrows(InvalidEmbeddingException.class,
                () -> matchingService.identify(request(null, 0.9)));
    }

    @Test
    @DisplayName("Every identify call is written to the audit log")
    void identifyWritesEvent() {
        IdentifyResponse response = matchingService.identify(
                request(demoDataService.createKnownProbe().embedding(), 0.97));

        assertNotNull(response.eventId());
        assertTrue(templateRepository.findRecentEvents(5).stream()
                .anyMatch(event -> event.id().equals(response.eventId())
                        && TEST_CAMERA.equals(event.cameraId())));
    }

    @Test
    @DisplayName("A newly enrolled subject becomes recognisable")
    void enrollThenIdentify() {
        Subject fresh = templateRepository.createSubject("Testov T.", "guest");
        float[] identityVector = VectorUtils.randomUnit(
                matchingProperties.getEmbeddingDimension(), new Random(99));

        EnrollResponse enrolled = matchingService.enroll(new EnrollRequest(
                fresh.id(), identityVector, null, MODALITIES, 0.93, null));

        assertEquals(fresh.id(), enrolled.subjectId());
        assertEquals(512, enrolled.embeddingDimension());
        assertEquals(1, enrolled.totalTemplatesForSubject());

        float[] probe = VectorUtils.perturb(identityVector, 0.039, new Random(100));
        IdentifyResponse response = matchingService.identify(request(probe, 0.97));

        assertEquals(Decision.GRANTED, response.decision(), response.note());
        assertEquals(fresh.id(), response.subjectId());
    }

    @Test
    @DisplayName("Enrolling against an unknown subject raises SubjectNotFoundException")
    void enrollUnknownSubjectFails() {
        EnrollRequest bad = new EnrollRequest(
                UUID.randomUUID(),
                VectorUtils.randomUnit(matchingProperties.getEmbeddingDimension(), new Random(5)),
                null, MODALITIES, 0.9, null);

        assertThrows(SubjectNotFoundException.class, () -> matchingService.enroll(bad));
    }

    @Test
    @DisplayName("EXPLAIN is unavailable under the test profile")
    void explainIsNotSupportedOnInMemoryBackend() {
        float[] probe = demoDataService.createStrangerProbe().embedding();

        assertThrows(ExplainNotSupportedException.class,
                () -> matchingService.explainSearch(probe, null));
    }
}
