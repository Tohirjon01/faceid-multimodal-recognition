package uz.tuit.faceid.recognition.repository.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uz.tuit.faceid.recognition.entity.FaceTemplate;
import uz.tuit.faceid.recognition.entity.MatchCandidate;
import uz.tuit.faceid.recognition.entity.RecognitionEvent;
import uz.tuit.faceid.recognition.entity.Subject;
import uz.tuit.faceid.recognition.entity.enums.Decision;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.util.VectorUtils;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL + pgvector store, used by the {@code preprod} and {@code prod} profiles.
 * <p>
 * JPA is deliberately not used: Hibernate has no {@code vector(512)} type and
 * mapping one through a custom {@code UserType} adds complexity for no benefit
 * here. With {@link NamedParameterJdbcTemplate} the vector travels as text and
 * is converted inside the SQL with {@code CAST(... AS vector)}.
 * <p>
 * <b>About the HNSW index.</b> It is only used when the query orders by
 * {@code ORDER BY embedding <=> probe}, i.e. by ascending distance. Writing
 * {@code ORDER BY 1 - (embedding <=> probe) DESC} makes the planner drop the
 * index and scan the whole table, so the similarity is computed in the
 * {@code SELECT} list only.
 */
@Repository
@Profile({"preprod", "prod"})
public class PostgresTemplateRepository implements TemplateRepository {

    private static final int RETENTION_YEARS = 3;

    private static final String SEARCH_SQL = """
            SELECT t.subject_id,
                   s.full_name,
                   1 - (t.embedding <=> CAST(:probe AS vector)) AS similarity
            FROM   faceid.face_templates t
            JOIN   faceid.subjects s ON s.id = t.subject_id
            WHERE  t.model_id = :modelId
            ORDER  BY t.embedding <=> CAST(:probe AS vector)
            LIMIT  :maxRows
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresTemplateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getBackendName() {
        return "PostgreSQL + pgvector (HNSW cosine)";
    }

    @Override
    public Subject createSubject(String fullName, String role) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate retentionUntil = LocalDate.now().plusYears(RETENTION_YEARS);
        String effectiveRole = role == null || role.isBlank() ? "employee" : role;

        jdbcTemplate.update("""
                INSERT INTO faceid.subjects (id, full_name, role, consent_given_at, retention_until)
                VALUES (:id, :fullName, :role, :consentGivenAt, :retentionUntil)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("fullName", fullName)
                .addValue("role", effectiveRole)
                .addValue("consentGivenAt", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .addValue("retentionUntil", retentionUntil));

        return new Subject(id, fullName, effectiveRole, now, retentionUntil);
    }

    @Override
    public List<Subject> findAllSubjects() {
        return jdbcTemplate.query("""
                SELECT id, full_name, role, consent_given_at, retention_until
                FROM   faceid.subjects
                ORDER  BY full_name
                """, (rs, rowNum) -> mapSubject(rs));
    }

    @Override
    public Optional<Subject> findSubjectById(UUID subjectId) {
        List<Subject> rows = jdbcTemplate.query("""
                SELECT id, full_name, role, consent_given_at, retention_until
                FROM   faceid.subjects
                WHERE  id = :id
                """, new MapSqlParameterSource("id", subjectId), (rs, rowNum) -> mapSubject(rs));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public int countSubjects() {
        Integer count = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT count(*) FROM faceid.subjects", Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public int countTemplates() {
        Integer count = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT count(*) FROM faceid.face_templates", Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public int countTemplatesBySubject(UUID subjectId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM faceid.face_templates WHERE subject_id = :subjectId",
                new MapSqlParameterSource("subjectId", subjectId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public UUID saveTemplate(FaceTemplate template) {
        UUID id = template.id() == null ? UUID.randomUUID() : template.id();
        jdbcTemplate.update("""
                INSERT INTO faceid.face_templates
                       (id, subject_id, model_id, embedding, quality_score, modalities, crop_uri)
                VALUES (:id, :subjectId, :modelId, CAST(:embedding AS vector),
                        :qualityScore, CAST(:modalities AS text[]), :cropUri)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("subjectId", template.subjectId())
                .addValue("modelId", template.modelId())
                .addValue("embedding", VectorUtils.toVectorLiteral(template.embedding()))
                .addValue("qualityScore", template.qualityScore())
                .addValue("modalities", VectorUtils.toTextArrayLiteral(template.modalities()))
                .addValue("cropUri", template.cropUri()));
        return id;
    }

    @Override
    public Optional<float[]> findAnyEmbeddingBySubject(UUID subjectId) {
        List<String> rows = jdbcTemplate.query("""
                SELECT embedding::text
                FROM   faceid.face_templates
                WHERE  subject_id = :subjectId
                LIMIT  1
                """, new MapSqlParameterSource("subjectId", subjectId),
                (rs, rowNum) -> rs.getString(1));

        return rows.isEmpty()
                ? Optional.empty()
                : Optional.of(VectorUtils.parseVectorLiteral(rows.get(0)));
    }

    @Override
    public List<MatchCandidate> searchNearest(float[] probe, int modelId, int topK) {
        // A subject may hold several templates, so more rows are pulled from the
        // index and the best similarity per subject is picked in Java. Using
        // DISTINCT ON instead would reduce how much the HNSW index can help.
        List<MatchCandidate> rawRows = jdbcTemplate.query(SEARCH_SQL, new MapSqlParameterSource()
                        .addValue("probe", VectorUtils.toVectorLiteral(probe))
                        .addValue("modelId", modelId)
                        .addValue("maxRows", Math.max(topK * 4, topK)),
                (rs, rowNum) -> new MatchCandidate(
                        rs.getObject("subject_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getDouble("similarity")));

        Map<UUID, MatchCandidate> bestBySubject = new LinkedHashMap<>();
        for (MatchCandidate candidate : rawRows) {
            MatchCandidate previous = bestBySubject.get(candidate.subjectId());
            if (previous == null || candidate.similarity() > previous.similarity()) {
                bestBySubject.put(candidate.subjectId(), candidate);
            }
        }

        List<MatchCandidate> candidates = new ArrayList<>(bestBySubject.values());
        candidates.sort(Comparator.comparingDouble(MatchCandidate::similarity).reversed());
        return candidates.size() > topK
                ? new ArrayList<>(candidates.subList(0, topK))
                : candidates;
    }

    @Override
    public long saveEvent(RecognitionEvent event) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO faceid.recognition_events
                       (occurred_at, subject_id, camera_id, model_id, decision,
                        match_similarity, liveness_score, expression,
                        expression_probs, modalities_used)
                VALUES (:occurredAt, :subjectId, :cameraId, :modelId, :decision,
                        :matchSimilarity, :livenessScore, :expression,
                        CAST(:expressionProbs AS real[]), CAST(:modalitiesUsed AS text[]))
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("occurredAt", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .addValue("subjectId", event.subjectId())
                .addValue("cameraId", event.cameraId())
                .addValue("modelId", event.modelId())
                .addValue("decision", event.decision().name())
                .addValue("matchSimilarity", event.matchSimilarity())
                .addValue("livenessScore", event.livenessScore())
                .addValue("expression", event.expression())
                .addValue("expressionProbs", VectorUtils.toRealArrayLiteral(event.expressionProbabilities()))
                .addValue("modalitiesUsed", VectorUtils.toTextArrayLiteral(event.modalitiesUsed())),
                Long.class);
        return id == null ? -1L : id;
    }

    @Override
    public List<RecognitionEvent> findRecentEvents(int limit) {
        return jdbcTemplate.query("""
                SELECT e.id, e.occurred_at, e.subject_id, s.full_name, e.camera_id, e.model_id,
                       e.decision, e.match_similarity, e.liveness_score, e.expression,
                       e.modalities_used
                FROM   faceid.recognition_events e
                LEFT   JOIN faceid.subjects s ON s.id = e.subject_id
                ORDER  BY e.occurred_at DESC, e.id DESC
                LIMIT  :maxRows
                """, new MapSqlParameterSource("maxRows", limit),
                (rs, rowNum) -> new RecognitionEvent(
                        rs.getLong("id"),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("subject_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("camera_id"),
                        rs.getInt("model_id"),
                        Decision.valueOf(rs.getString("decision")),
                        readNullableDouble(rs, "match_similarity"),
                        readNullableDouble(rs, "liveness_score"),
                        rs.getString("expression"),
                        null,
                        readTextArray(rs, "modalities_used")));
    }

    @Override
    public String explainSearch(float[] probe, int modelId, int topK) {
        List<String> planLines = jdbcTemplate.query(
                "EXPLAIN (ANALYZE, COSTS OFF, TIMING ON) " + SEARCH_SQL,
                new MapSqlParameterSource()
                        .addValue("probe", VectorUtils.toVectorLiteral(probe))
                        .addValue("modelId", modelId)
                        .addValue("maxRows", Math.max(topK * 4, topK)),
                (rs, rowNum) -> rs.getString(1));
        return String.join("\n", planLines);
    }

    // ── row mappers ────────────────────────────────────────────

    private static Subject mapSubject(ResultSet rs) throws SQLException {
        OffsetDateTime consentGivenAt = rs.getObject("consent_given_at", OffsetDateTime.class);
        LocalDate retentionUntil = rs.getObject("retention_until", LocalDate.class);
        return new Subject(
                rs.getObject("id", UUID.class),
                rs.getString("full_name"),
                rs.getString("role"),
                consentGivenAt == null ? null : consentGivenAt.toInstant(),
                retentionUntil);
    }

    /**
     * {@code getObject} returns Float or Double depending on the column type,
     * so casting directly to {@code (Double)} can throw ClassCastException.
     */
    private static Double readNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static List<String> readTextArray(ResultSet rs, String column) throws SQLException {
        Array array = rs.getArray(column);
        if (array == null) {
            return List.of();
        }
        Object raw = array.getArray();
        return raw instanceof String[] values ? List.of(values) : List.of();
    }
}
