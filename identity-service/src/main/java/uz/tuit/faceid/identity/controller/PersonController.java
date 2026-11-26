package uz.tuit.faceid.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.identity.dto.request.PersonCreateRequest;
import uz.tuit.faceid.identity.dto.response.PersonResponse;
import uz.tuit.faceid.identity.dto.response.RegistrySummaryResponse;
import uz.tuit.faceid.identity.service.PersonService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "Persons", description = "Person registry")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a person",
            description = "Consent timestamp and retention date are set automatically")
    public PersonResponse register(@Valid @RequestBody PersonCreateRequest request) {
        return personService.register(request);
    }

    @GetMapping
    @Operation(summary = "List all people", description = "Sorted by name")
    public List<PersonResponse> findAll() {
        return personService.findAll();
    }

    /**
     * Declared before {@code /{personId}} on purpose: otherwise Spring would
     * try to parse "expiring" as a UUID.
     */
    @GetMapping("/expiring")
    @Operation(summary = "Expiring soon",
            description = "Compliance report; people who withdrew consent are excluded")
    public List<PersonResponse> findExpiring(
            @Parameter(description = "Window in days; falls back to the configured default")
            @RequestParam(required = false) Integer days) {
        return personService.findExpiringWithin(days);
    }

    @GetMapping("/summary")
    @Operation(summary = "Compliance summary for the registry")
    public RegistrySummaryResponse summary() {
        return personService.summary();
    }

    @GetMapping("/{personId}")
    @Operation(summary = "Fetch one person")
    public PersonResponse findById(@PathVariable UUID personId) {
        return personService.findById(personId);
    }

    @DeleteMapping("/{personId}")
    @Operation(summary = "Right to erasure",
            description = "Removes the person record entirely")
    public ResponseEntity<Void> erase(@PathVariable UUID personId) {
        return personService.erase(personId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
