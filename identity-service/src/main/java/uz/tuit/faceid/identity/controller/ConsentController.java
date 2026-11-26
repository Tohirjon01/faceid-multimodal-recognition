package uz.tuit.faceid.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.identity.dto.response.ConsentResponse;
import uz.tuit.faceid.identity.service.ConsentService;

import java.util.UUID;

/**
 * Biometric consent. A separate controller, because consent is a legal process
 * and needs an audit trail distinct from ordinary profile edits.
 */
@RestController
@RequestMapping("/api/v1/subjects/{personId}/consent")
@Tag(name = "Consent", description = "Biometric consent — ZRU-547")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    @Operation(summary = "Current consent state")
    public ConsentResponse getConsent(@PathVariable UUID personId) {
        return consentService.getConsent(personId);
    }

    @PostMapping("/revoke")
    @Operation(summary = "Withdraw consent",
            description = "After this, the stored biometric templates must be deleted")
    public ConsentResponse revoke(@PathVariable UUID personId) {
        return consentService.revoke(personId);
    }

    @PostMapping("/renew")
    @Operation(summary = "Renew consent",
            description = "The retention period is recalculated from today")
    public ConsentResponse renew(@PathVariable UUID personId) {
        return consentService.renew(personId);
    }
}
