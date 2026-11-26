package uz.tuit.faceid.recognition.repository;

import uz.tuit.faceid.recognition.entity.FaceTemplate;
import uz.tuit.faceid.recognition.entity.MatchCandidate;
import uz.tuit.faceid.recognition.entity.RecognitionEvent;
import uz.tuit.faceid.recognition.entity.Subject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Template store. Two implementations, selected by the active profile:
 *
 * <ul>
 *   <li>{@code test}                  → {@code InMemoryTemplateRepository}, no database required</li>
 *   <li>{@code preprod}, {@code prod} → {@code PostgresTemplateRepository}, pgvector + HNSW</li>
 * </ul>
 *
 * The REST contract is identical in both cases, so the layers above never need
 * to know which backend is active.
 */
public interface TemplateRepository {

    /** Backend name, surfaced in responses and startup logs. */
    String getBackendName();

    Subject createSubject(String fullName, String role);

    List<Subject> findAllSubjects();

    Optional<Subject> findSubjectById(UUID subjectId);

    int countSubjects();

    int countTemplates();

    int countTemplatesBySubject(UUID subjectId);

    UUID saveTemplate(FaceTemplate template);

    /** Returns any one template of the given subject (used to build demo probes). */
    Optional<float[]> findAnyEmbeddingBySubject(UUID subjectId);

    /**
     * Returns the {@code topK} nearest subjects by cosine similarity.
     * When a subject holds several templates, the highest similarity wins.
     */
    List<MatchCandidate> searchNearest(float[] probe, int modelId, int topK);

    long saveEvent(RecognitionEvent event);

    List<RecognitionEvent> findRecentEvents(int limit);

    /** Query execution plan. Returns {@code null} on the in-memory backend. */
    String explainSearch(float[] probe, int modelId, int topK);
}
