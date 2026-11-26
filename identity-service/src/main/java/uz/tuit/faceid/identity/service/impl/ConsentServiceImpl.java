package uz.tuit.faceid.identity.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.tuit.faceid.identity.config.properties.IdentityProperties;
import uz.tuit.faceid.identity.dto.response.ConsentResponse;
import uz.tuit.faceid.identity.entity.Person;
import uz.tuit.faceid.identity.exception.PersonNotFoundException;
import uz.tuit.faceid.identity.mapper.PersonMapper;
import uz.tuit.faceid.identity.repository.PersonRepository;
import uz.tuit.faceid.identity.service.ConsentService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ConsentServiceImpl implements ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentServiceImpl.class);

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final IdentityProperties identityProperties;
    private final Clock clock;

    public ConsentServiceImpl(PersonRepository personRepository,
                              PersonMapper personMapper,
                              IdentityProperties identityProperties,
                              Clock clock) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
        this.identityProperties = identityProperties;
        this.clock = clock;
    }

    @Override
    public ConsentResponse getConsent(UUID personId) {
        return personMapper.toConsentResponse(requirePerson(personId), LocalDate.now(clock));
    }

    @Override
    public ConsentResponse revoke(UUID personId) {
        Person updated = requirePerson(personId).withConsentRevoked(clock.instant());
        personRepository.save(updated);

        // Next step: emit an event here so recognition-service deletes every
        // biometric template belonging to this person.
        log.warn("Consent revoked: {} — biometric templates must be deleted", personId);

        return personMapper.toConsentResponse(updated, LocalDate.now(clock));
    }

    @Override
    public ConsentResponse renew(UUID personId) {
        LocalDate today = LocalDate.now(clock);
        Person updated = requirePerson(personId).withConsentRenewed(
                clock.instant(), today.plusYears(identityProperties.getRetentionYears()));
        personRepository.save(updated);

        log.info("Consent renewed: {} — new retention date {}", personId, updated.retentionUntil());
        return personMapper.toConsentResponse(updated, today);
    }

    private Person requirePerson(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
    }
}
