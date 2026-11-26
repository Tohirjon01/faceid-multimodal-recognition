package uz.tuit.faceid.recognition.repository.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import uz.tuit.faceid.recognition.entity.FaceTemplate;
import uz.tuit.faceid.recognition.entity.MatchCandidate;
import uz.tuit.faceid.recognition.entity.RecognitionEvent;
import uz.tuit.faceid.recognition.entity.Subject;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.util.VectorUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory store, used by the {@code test} profile.
 * <p>
 * The search is linear: O(N·d) for N templates. At 30–1000 templates that is a
 * matter of milliseconds, which is plenty for tests and demos. Millions of
 * templates need HNSW, i.e. the {@code preprod}/{@code prod} profile.
 */
@Repository
@Profile("test")
public class InMemoryTemplateRepository implements TemplateRepository {

    private static final int RETENTION_YEARS = 3;

    private final Map<UUID, Subject> subjects = new ConcurrentHashMap<>();
    private final List<FaceTemplate> templates = new CopyOnWriteArrayList<>();
    private final List<RecognitionEvent> events = new CopyOnWriteArrayList<>();
    private final AtomicLong eventSequence = new AtomicLong(1);

    @Override
    public String getBackendName() {
        return "in-memory (cosine in Java)";
    }

    @Override
    public Subject createSubject(String fullName, String role) {
        Subject subject = new Subject(
                UUID.randomUUID(),
                fullName,
                role == null || role.isBlank() ? "employee" : role,
                Instant.now(),
                LocalDate.now().plusYears(RETENTION_YEARS)
        );
        subjects.put(subject.id(), subject);
        return subject;
    }

    @Override
    public List<Subject> findAllSubjects() {
        return subjects.values().stream()
                .sorted(Comparator.comparing(Subject::fullName))
                .toList();
    }

    @Override
    public Optional<Subject> findSubjectById(UUID subjectId) {
        return Optional.ofNullable(subjects.get(subjectId));
    }

    @Override
    public int countSubjects() {
        return subjects.size();
    }

    @Override
    public int countTemplates() {
        return templates.size();
    }

    @Override
    public int countTemplatesBySubject(UUID subjectId) {
        return (int) templates.stream()
                .filter(template -> template.subjectId().equals(subjectId))
                .count();
    }

    @Override
    public UUID saveTemplate(FaceTemplate template) {
        UUID id = template.id() == null ? UUID.randomUUID() : template.id();
        templates.add(new FaceTemplate(
                id,
                template.subjectId(),
                template.modelId(),
                template.embedding(),
                template.qualityScore(),
                template.modalities(),
                template.cropUri()
        ));
        return id;
    }

    @Override
    public Optional<float[]> findAnyEmbeddingBySubject(UUID subjectId) {
        return templates.stream()
                .filter(template -> template.subjectId().equals(subjectId))
                .map(FaceTemplate::embedding)
                .findFirst();
    }

    @Override
    public List<MatchCandidate> searchNearest(float[] probe, int modelId, int topK) {
        Map<UUID, Double> bestBySubject = new HashMap<>();
        for (FaceTemplate template : templates) {
            if (template.modelId() != modelId) {
                continue;
            }
            double similarity = VectorUtils.cosine(probe, template.embedding());
            bestBySubject.merge(template.subjectId(), similarity, Math::max);
        }

        List<MatchCandidate> candidates = new ArrayList<>(bestBySubject.size());
        for (Map.Entry<UUID, Double> entry : bestBySubject.entrySet()) {
            Subject subject = subjects.get(entry.getKey());
            candidates.add(new MatchCandidate(
                    entry.getKey(),
                    subject == null ? "(unnamed)" : subject.fullName(),
                    entry.getValue()
            ));
        }
        candidates.sort(Comparator.comparingDouble(MatchCandidate::similarity).reversed());
        return candidates.size() > topK
                ? new ArrayList<>(candidates.subList(0, topK))
                : candidates;
    }

    @Override
    public long saveEvent(RecognitionEvent event) {
        long id = eventSequence.getAndIncrement();
        events.add(new RecognitionEvent(
                id,
                event.occurredAt(),
                event.subjectId(),
                event.fullName(),
                event.cameraId(),
                event.modelId(),
                event.decision(),
                event.matchSimilarity(),
                event.livenessScore(),
                event.expression(),
                event.expressionProbabilities(),
                event.modalitiesUsed()
        ));
        return id;
    }

    @Override
    public List<RecognitionEvent> findRecentEvents(int limit) {
        List<RecognitionEvent> snapshot = new ArrayList<>(events);
        snapshot.sort(Comparator.comparing(RecognitionEvent::id).reversed());
        return snapshot.size() > limit
                ? new ArrayList<>(snapshot.subList(0, limit))
                : snapshot;
    }

    @Override
    public String explainSearch(float[] probe, int modelId, int topK) {
        return null;
    }
}
