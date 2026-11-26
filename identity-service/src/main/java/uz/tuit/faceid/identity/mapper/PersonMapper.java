package uz.tuit.faceid.identity.mapper;

import org.springframework.stereotype.Component;
import uz.tuit.faceid.identity.dto.response.ConsentResponse;
import uz.tuit.faceid.identity.dto.response.PersonResponse;
import uz.tuit.faceid.identity.entity.Person;

import java.time.LocalDate;
import java.util.List;

@Component
public class PersonMapper {

    public PersonResponse toResponse(Person person, LocalDate today) {
        return new PersonResponse(
                person.id(),
                person.fullName(),
                person.role().getCode(),
                person.resolveConsentStatus(today),
                person.consentGivenAt(),
                person.retentionUntil());
    }

    public List<PersonResponse> toResponseList(List<Person> people, LocalDate today) {
        return people.stream().map(person -> toResponse(person, today)).toList();
    }

    public ConsentResponse toConsentResponse(Person person, LocalDate today) {
        return new ConsentResponse(
                person.id(),
                person.resolveConsentStatus(today),
                person.consentGivenAt(),
                person.consentRevokedAt(),
                person.retentionUntil(),
                person.isConsentValid(today));
    }
}
