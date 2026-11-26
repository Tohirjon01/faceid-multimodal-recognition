package uz.tuit.faceid.identity.service;

import uz.tuit.faceid.identity.dto.request.PersonCreateRequest;
import uz.tuit.faceid.identity.dto.response.PersonResponse;
import uz.tuit.faceid.identity.dto.response.RegistrySummaryResponse;

import java.util.List;
import java.util.UUID;

/** Person registry. */
public interface PersonService {

    PersonResponse register(PersonCreateRequest request);

    PersonResponse findById(UUID personId);

    List<PersonResponse> findAll();

    /** Right to erasure — the record is removed entirely. */
    boolean erase(UUID personId);

    /** People whose retention period ends within {@code days}. */
    List<PersonResponse> findExpiringWithin(Integer days);

    RegistrySummaryResponse summary();
}
