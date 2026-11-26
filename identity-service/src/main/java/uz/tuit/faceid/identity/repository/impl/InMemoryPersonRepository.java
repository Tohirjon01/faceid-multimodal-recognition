package uz.tuit.faceid.identity.repository.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import uz.tuit.faceid.identity.entity.Person;
import uz.tuit.faceid.identity.repository.PersonRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store, used by the {@code test} profile. */
@Repository
@Profile("test")
public class InMemoryPersonRepository implements PersonRepository {

    private final Map<UUID, Person> people = new ConcurrentHashMap<>();

    @Override
    public String getBackendName() {
        return "in-memory";
    }

    @Override
    public Person save(Person person) {
        people.put(person.id(), person);
        return person;
    }

    @Override
    public Optional<Person> findById(UUID personId) {
        return Optional.ofNullable(people.get(personId));
    }

    @Override
    public List<Person> findAll() {
        return people.values().stream()
                .sorted(Comparator.comparing(Person::fullName))
                .toList();
    }

    @Override
    public List<Person> findExpiringUntil(LocalDate limit) {
        return people.values().stream()
                .filter(person -> person.consentRevokedAt() == null)
                .filter(person -> !person.retentionUntil().isAfter(limit))
                .sorted(Comparator.comparing(Person::retentionUntil))
                .toList();
    }

    @Override
    public boolean deleteById(UUID personId) {
        return people.remove(personId) != null;
    }

    @Override
    public int count() {
        return people.size();
    }
}
