package uz.tuit.faceid.recognition.service.impl;

import org.springframework.stereotype.Service;
import uz.tuit.faceid.recognition.config.properties.MatchingProperties;
import uz.tuit.faceid.recognition.dto.request.EnrollRequest;
import uz.tuit.faceid.recognition.dto.request.IdentifyRequest;
import uz.tuit.faceid.recognition.dto.response.EnrollResponse;
import uz.tuit.faceid.recognition.dto.response.IdentifyResponse;
import uz.tuit.faceid.recognition.entity.DecisionOutcome;
import uz.tuit.faceid.recognition.entity.FaceTemplate;
import uz.tuit.faceid.recognition.entity.MatchCandidate;
import uz.tuit.faceid.recognition.entity.RecognitionEvent;
import uz.tuit.faceid.recognition.entity.enums.Decision;
import uz.tuit.faceid.recognition.entity.enums.Expression;
import uz.tuit.faceid.recognition.exception.ExplainNotSupportedException;
import uz.tuit.faceid.recognition.exception.InvalidEmbeddingException;
import uz.tuit.faceid.recognition.exception.SubjectNotFoundException;
import uz.tuit.faceid.recognition.mapper.CandidateMapper;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.DecisionPolicy;
import uz.tuit.faceid.recognition.service.MatchingService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MatchingServiceImpl implements MatchingService {

    private final TemplateRepository templateRepository;
    private final DecisionPolicy decisionPolicy;
    private final CandidateMapper candidateMapper;
    private final MatchingProperties matchingProperties;

    public MatchingServiceImpl(TemplateRepository templateRepository,
                               DecisionPolicy decisionPolicy,
                               CandidateMapper candidateMapper,
                               MatchingProperties matchingProperties) {
        this.templateRepository = templateRepository;
        this.decisionPolicy = decisionPolicy;
        this.candidateMapper = candidateMapper;
        this.matchingProperties = matchingProperties;
    }

    @Override
    public IdentifyResponse identify(IdentifyRequest request) {
        long startedAt = System.nanoTime();

        float[] probe = requireValidEmbedding(request.embedding());
        int modelId = resolveModelId(request.modelId());

        long searchStartedAt = System.nanoTime();
        List<MatchCandidate> candidates = templateRepository.searchNearest(
                probe, modelId, matchingProperties.getTopK());
        double searchMs = millisSince(searchStartedAt);

        DecisionOutcome outcome = decisionPolicy.evaluate(request.livenessScore(), candidates);
        String expressionLabel = Expression.labelOf(
                Expression.argmax(request.expressionProbabilities()));

        // The subject is attached only on GRANTED. On SPOOF the subject may well
        // have been recognised, but the audit log must not carry a trace that
        // reads as "this person entered".
        UUID matchedSubjectId = outcome.decision() == Decision.GRANTED
                ? outcome.bestCandidate().subjectId()
                : null;
        String matchedFullName = matchedSubjectId == null
                ? null
                : outcome.bestCandidate().fullName();

        long eventId = templateRepository.saveEvent(new RecognitionEvent(
                null,
                Instant.now(),
                matchedSubjectId,
                matchedFullName,
                request.cameraId() == null ? "unknown" : request.cameraId(),
                modelId,
                outcome.decision(),
                outcome.bestCandidate() == null ? null : outcome.bestCandidate().similarity(),
                request.livenessScore(),
                expressionLabel,
                request.expressionProbabilities(),
                request.modalitiesUsed() == null ? List.of() : request.modalitiesUsed()
        ));

        return new IdentifyResponse(
                outcome.decision(),
                matchedSubjectId,
                matchedFullName,
                outcome.bestCandidate() == null ? null : outcome.bestCandidate().similarity(),
                matchingProperties.getSimilarityThreshold(),
                expressionLabel,
                candidateMapper.toResponseList(candidates, matchingProperties.getSimilarityThreshold()),
                searchMs,
                millisSince(startedAt),
                eventId,
                templateRepository.getBackendName(),
                outcome.note()
        );
    }

    @Override
    public EnrollResponse enroll(EnrollRequest request) {
        templateRepository.findSubjectById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));

        float[] embedding = requireValidEmbedding(request.embedding());
        int modelId = resolveModelId(request.modelId());

        UUID templateId = templateRepository.saveTemplate(new FaceTemplate(
                null,
                request.subjectId(),
                modelId,
                embedding,
                request.qualityScore(),
                request.modalities() == null ? List.of("rgb") : request.modalities(),
                request.cropUri()
        ));

        return new EnrollResponse(
                templateId,
                request.subjectId(),
                modelId,
                embedding.length,
                templateRepository.countTemplatesBySubject(request.subjectId())
        );
    }

    @Override
    public String explainSearch(float[] probe, Integer modelId) {
        String plan = templateRepository.explainSearch(
                requireValidEmbedding(probe),
                resolveModelId(modelId),
                matchingProperties.getTopK());

        if (plan == null) {
            throw new ExplainNotSupportedException(templateRepository.getBackendName());
        }
        return plan;
    }

    // ── helpers ────────────────────────────────────────────────

    private float[] requireValidEmbedding(float[] embedding) {
        if (embedding == null) {
            throw InvalidEmbeddingException.missing();
        }
        if (embedding.length != matchingProperties.getEmbeddingDimension()) {
            throw InvalidEmbeddingException.wrongDimension(
                    matchingProperties.getEmbeddingDimension(), embedding.length);
        }
        return embedding;
    }

    private int resolveModelId(Integer requested) {
        return requested == null ? matchingProperties.getActiveModelId() : requested;
    }

    /** Milliseconds with three decimals — the in-memory search takes well under 1 ms. */
    private static double millisSince(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 1_000.0) / 1_000.0;
    }
}
