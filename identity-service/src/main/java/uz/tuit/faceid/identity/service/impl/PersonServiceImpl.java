package uz.tuit.faceid.identity.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.tuit.faceid.identity.config.properties.IdentityProperties;
import uz.tuit.faceid.identity.dto.request.PersonCreateRequest;
import uz.tuit.faceid.identity.dto.response.PersonResponse;
import uz.tuit.faceid.identity.dto.response.RegistrySummaryResponse;
import uz.tuit.faceid.identity.entity.Person;
import uz.tuit.faceid.identity.entity.enums.ConsentStatus;
import uz.tuit.faceid.identity.entity.enums.PersonRole;
import uz.tuit.faceid.identity.exception.PersonNotFoundException;
import uz.tuit.faceid.identity.mapper.PersonMapper;
import uz.tuit.faceid.identity.repository.PersonRepository;
import uz.tuit.faceid.identity.service.PersonService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PersonServiceImpl implements PersonService {

    private static final Logger log = LoggerFactory.getLogger(PersonServiceImpl.class);

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final IdentityProperties identityProperties;
    private final Clock clock;

    public PersonServiceImpl(PersonRepository personRepository,
                             PersonMapper personMapper,
                             IdentityProperties identityProperties,
                             Clock clock) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
        this.identityProperties = identityProperties;
        this.clock = clock;
    }

    @Override
    public PersonResponse register(PersonCreateRequest request) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);

        Person person = new Person(
                UUID.randomUUID(),
                request.fullName(),
                PersonRole.fromCode(request.role()),
                now,
                null,
                today.plusYears(identityProperties.getRetentionYears()),
                now
        );

        personRepository.save(person);
        log.info("Person registered: {} ({})", person.id(), person.role().getCode());
        return personMapper.toResponse(person, today);
    }

    @Override
    public PersonResponse findById(UUID personId) {
        return personMapper.toResponse(requirePerson(personId), LocalDate.now(clock));
    }

    @Override
    public List<PersonResponse> findAll() {
        return personMapper.toResponseList(personRepository.findAll(), LocalDate.now(clock));
    }

    @Override
    public boolean erase(UUID personId) {
        boolean removed = personRepository.deleteById(personId);
        if (removed) {
            log.info("Person erased (right to erasure): {}", personId);
        }
        return removed;
    }

    @Override
    public List<PersonResponse> findExpiringWithin(Integer days) {
        int window = days == null ? identityProperties.getDefaultExpiryWindowDays() : days;
        LocalDate today = LocalDate.now(clock);
        return personMapper.toResponseList(
                personRepository.findExpiringUntil(today.plusDays(window)), today);
    }

    @Override
    public RegistrySummaryResponse summary() {
        LocalDate today = LocalDate.now(clock);
        List<Person> all = personRepository.findAll();

        return new RegistrySummaryResponse(
                all.size(),
                countByStatus(all, today, ConsentStatus.ACTIVE),
                countByStatus(all, today, ConsentStatus.REVOKED),
                countByStatus(all, today, ConsentStatus.EXPIRED));
    }

    private Person requirePerson(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
    }

    private static long countByStatus(List<Person> people, LocalDate today, ConsentStatus status) {
        return people.stream()
                .filter(person -> person.resolveConsentStatus(today) == status)
                .count();
    }
}
