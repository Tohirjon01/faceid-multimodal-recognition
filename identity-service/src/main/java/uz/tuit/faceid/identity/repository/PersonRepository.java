package uz.tuit.faceid.identity.repository;

import uz.tuit.faceid.identity.entity.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Person store. Two implementations, selected by the active profile:
 *
 * <ul>
 *   <li>{@code test}                  → in-memory, no database required</li>
 *   <li>{@code preprod}, {@code prod} → PostgreSQL, {@code identity} schema</li>
 * </ul>
 */
public interface PersonRepository {

    String getBackendName();

    Person save(Person person);

    Optional<Person> findById(UUID personId);

    List<Person> findAll();

    /** People whose retention ends on or before the given date and whose consent still stands. */
    List<Person> findExpiringUntil(LocalDate limit);

    boolean deleteById(UUID personId);

    int count();
}
