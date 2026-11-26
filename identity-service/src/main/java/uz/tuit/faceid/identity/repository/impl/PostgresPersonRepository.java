package uz.tuit.faceid.identity.repository.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uz.tuit.faceid.identity.entity.Person;
import uz.tuit.faceid.identity.entity.enums.PersonRole;
import uz.tuit.faceid.identity.repository.PersonRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL store, used by the {@code preprod} and {@code prod} profiles.
 * <p>
 * {@code save} behaves as an upsert: a new record is inserted, an existing one
 * has its consent fields refreshed. That keeps the service layer from having to
 * distinguish "create" from "update".
 */
@Repository
@Profile({"preprod", "prod"})
public class PostgresPersonRepository implements PersonRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, full_name, role, consent_given_at, consent_revoked_at,
                   retention_until, created_at
            FROM   identity.persons
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresPersonRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getBackendName() {
        return "PostgreSQL (identity schema)";
    }

    @Override
    public Person save(Person person) {
        jdbcTemplate.update("""
                INSERT INTO identity.persons
                       (id, full_name, role, consent_given_at, consent_revoked_at,
                        retention_until, created_at)
                VALUES (:id, :fullName, :role, :consentGivenAt, :consentRevokedAt,
                        :retentionUntil, :createdAt)
                ON CONFLICT (id) DO UPDATE SET
                       full_name          = EXCLUDED.full_name,
                       role               = EXCLUDED.role,
                       consent_given_at   = EXCLUDED.consent_given_at,
                       consent_revoked_at = EXCLUDED.consent_revoked_at,
                       retention_until    = EXCLUDED.retention_until
                """, toParameters(person));
        return person;
    }

    @Override
    public Optional<Person> findById(UUID personId) {
        List<Person> rows = jdbcTemplate.query(SELECT_COLUMNS + " WHERE id = :id",
                new MapSqlParameterSource("id", personId), (rs, rowNum) -> mapPerson(rs));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<Person> findAll() {
        return jdbcTemplate.query(SELECT_COLUMNS + " ORDER BY full_name",
                (rs, rowNum) -> mapPerson(rs));
    }

    @Override
    public List<Person> findExpiringUntil(LocalDate limit) {
        return jdbcTemplate.query(SELECT_COLUMNS + """
                 WHERE consent_revoked_at IS NULL
                   AND retention_until <= :limit
                 ORDER BY retention_until
                """, new MapSqlParameterSource("limit", limit), (rs, rowNum) -> mapPerson(rs));
    }

    @Override
    public boolean deleteById(UUID personId) {
        return jdbcTemplate.update("DELETE FROM identity.persons WHERE id = :id",
                new MapSqlParameterSource("id", personId)) > 0;
    }

    @Override
    public int count() {
        Integer count = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT count(*) FROM identity.persons", Integer.class);
        return count == null ? 0 : count;
    }

    // ── row mappers ────────────────────────────────────────────

    private static MapSqlParameterSource toParameters(Person person) {
        return new MapSqlParameterSource()
                .addValue("id", person.id())
                .addValue("fullName", person.fullName())
                .addValue("role", person.role().getCode())
                .addValue("consentGivenAt", toOffsetDateTime(person.consentGivenAt()))
                .addValue("consentRevokedAt", toOffsetDateTime(person.consentRevokedAt()))
                .addValue("retentionUntil", person.retentionUntil())
                .addValue("createdAt", toOffsetDateTime(person.createdAt()));
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Person mapPerson(ResultSet rs) throws SQLException {
        OffsetDateTime consentGivenAt = rs.getObject("consent_given_at", OffsetDateTime.class);
        OffsetDateTime consentRevokedAt = rs.getObject("consent_revoked_at", OffsetDateTime.class);
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);

        return new Person(
                rs.getObject("id", UUID.class),
                rs.getString("full_name"),
                PersonRole.fromCode(rs.getString("role")),
                consentGivenAt == null ? null : consentGivenAt.toInstant(),
                consentRevokedAt == null ? null : consentRevokedAt.toInstant(),
                rs.getObject("retention_until", LocalDate.class),
                createdAt == null ? null : createdAt.toInstant());
    }
}
