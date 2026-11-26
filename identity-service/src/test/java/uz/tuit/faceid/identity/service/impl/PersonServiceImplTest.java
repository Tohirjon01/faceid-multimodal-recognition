package uz.tuit.faceid.identity.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uz.tuit.faceid.identity.config.properties.IdentityProperties;
import uz.tuit.faceid.identity.dto.request.PersonCreateRequest;
import uz.tuit.faceid.identity.dto.response.ConsentResponse;
import uz.tuit.faceid.identity.dto.response.PersonResponse;
import uz.tuit.faceid.identity.dto.response.RegistrySummaryResponse;
import uz.tuit.faceid.identity.entity.enums.ConsentStatus;
import uz.tuit.faceid.identity.exception.PersonNotFoundException;
import uz.tuit.faceid.identity.mapper.PersonMapper;
import uz.tuit.faceid.identity.repository.impl.InMemoryPersonRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consent and retention logic.
 * <p>
 * A fixed {@link Clock} is used; otherwise conditions such as "three years
 * from now" could not be asserted at all.
 */
@DisplayName("PersonService and ConsentService — consent logic")
class PersonServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-20T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final LocalDate RETENTION_END = LocalDate.of(2029, 10, 20);

    private InMemoryPersonRepository repository;
    private PersonServiceImpl personService;
    private ConsentServiceImpl consentService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPersonRepository();
        PersonMapper mapper = new PersonMapper();
        IdentityProperties properties = new IdentityProperties();

        personService = new PersonServiceImpl(repository, mapper, properties, FIXED_CLOCK);
        consentService = new ConsentServiceImpl(repository, mapper, properties, FIXED_CLOCK);
    }

    private PersonResponse register(String name, String role) {
        return personService.register(new PersonCreateRequest(name, role));
    }

    @Test
    @DisplayName("Registration records consent and a three-year retention period")
    void registerSetsConsentAndRetention() {
        PersonResponse person = register("Sadullayev T.", "employee");

        assertNotNull(person.id());
        assertEquals(NOW, person.consentGivenAt());
        assertEquals(RETENTION_END, person.retentionUntil());
        assertEquals(ConsentStatus.ACTIVE, person.consentStatus());
    }

    @Test
    @DisplayName("An unknown or blank role falls back to employee")
    void unknownRoleFallsBackToEmployee() {
        assertEquals("employee", register("Testov T.", null).role());
        assertEquals("employee", register("Testov B.", "  ").role());
        assertEquals("employee", register("Testov D.", "superadmin").role());
        assertEquals("security", register("Testov S.", "SECURITY").role());
    }

    @Test
    @DisplayName("Withdrawn consent reads as REVOKED and is no longer valid")
    void revokedConsentIsInvalid() {
        PersonResponse person = register("Karimova A.", "manager");

        ConsentResponse consent = consentService.revoke(person.id());

        assertEquals(ConsentStatus.REVOKED, consent.status());
        assertEquals(NOW, consent.revokedAt());
        assertFalse(consent.valid());
    }

    @Test
    @DisplayName("Renewing consent resets the retention period and clears the withdrawal")
    void renewResetsRetentionAndClearsRevocation() {
        PersonResponse person = register("Alimov E.", "employee");
        consentService.revoke(person.id());

        ConsentResponse renewed = consentService.renew(person.id());

        assertEquals(ConsentStatus.ACTIVE, renewed.status());
        assertNull(renewed.revokedAt());
        assertTrue(renewed.valid());
        assertEquals(RETENTION_END, renewed.retentionUntil());
    }

    @Test
    @DisplayName("Erasure removes the record completely")
    void eraseRemovesPerson() {
        PersonResponse person = register("Nazarov O.", "guest");

        assertTrue(personService.erase(person.id()));
        assertThrows(PersonNotFoundException.class, () -> personService.findById(person.id()));
        assertFalse(personService.erase(person.id()), "a second erase must return false");
    }

    @Test
    @DisplayName("An unknown person raises PersonNotFoundException")
    void unknownPersonThrows() {
        UUID missing = UUID.randomUUID();

        PersonNotFoundException exception = assertThrows(PersonNotFoundException.class,
                () -> consentService.getConsent(missing));
        assertTrue(exception.getMessage().contains(missing.toString()));
    }

    @Test
    @DisplayName("The expiring report excludes people who withdrew consent")
    void expiringExcludesRevoked() {
        PersonResponse active = register("Active A.", "employee");
        PersonResponse revoked = register("Revoked R.", "employee");
        consentService.revoke(revoked.id());

        // Three years plus a margin — both would otherwise fall inside the window.
        var expiring = personService.findExpiringWithin(365 * 3 + 2);

        assertTrue(expiring.stream().anyMatch(p -> p.id().equals(active.id())));
        assertFalse(expiring.stream().anyMatch(p -> p.id().equals(revoked.id())));
    }

    @Test
    @DisplayName("Nothing expiring soon gives an empty list")
    void nothingExpiringSoon() {
        register("Fresh F.", "employee");

        assertTrue(personService.findExpiringWithin(30).isEmpty());
    }

    @Test
    @DisplayName("The summary counts by consent status")
    void summaryCountsByStatus() {
        register("Active A.", "employee");
        register("Active B.", "employee");
        PersonResponse revoked = register("Revoked R.", "guest");
        consentService.revoke(revoked.id());

        RegistrySummaryResponse summary = personService.summary();

        assertEquals(3, summary.total());
        assertEquals(2, summary.active());
        assertEquals(1, summary.revoked());
        assertEquals(0, summary.expired());
    }

    @Test
    @DisplayName("The listing is sorted by name")
    void findAllIsSortedByName() {
        register("Yusupov B.", "employee");
        register("Alimov E.", "employee");

        assertEquals("Alimov E.", personService.findAll().get(0).fullName());
        assertEquals(2, repository.count());
    }

    @Test
    @DisplayName("An elapsed retention period reads as EXPIRED")
    void expiredRetentionIsReportedAsExpired() {
        PersonResponse person = register("Old O.", "employee");

        // Move the clock three years forward: new service, same repository.
        Clock future = Clock.fixed(NOW.plusSeconds(3L * 366 * 24 * 3600), ZoneOffset.UTC);
        PersonServiceImpl futureService = new PersonServiceImpl(
                repository, new PersonMapper(), new IdentityProperties(), future);

        assertEquals(ConsentStatus.EXPIRED, futureService.findById(person.id()).consentStatus());
        assertEquals(1, futureService.summary().expired());
    }

    @Test
    @DisplayName("Consent is still valid on the final day of the retention period")
    void retentionIsValidOnLastDay() {
        PersonResponse person = register("Edge E.", "employee");

        assertEquals(RETENTION_END, person.retentionUntil());
        assertTrue(consentService.getConsent(person.id()).valid());
        assertEquals(TODAY.plusYears(3), person.retentionUntil());
    }
}
