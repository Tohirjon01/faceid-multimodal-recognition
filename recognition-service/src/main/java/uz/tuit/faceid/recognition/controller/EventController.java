package uz.tuit.faceid.recognition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.recognition.dto.response.EventResponse;
import uz.tuit.faceid.recognition.service.EventService;

import java.util.List;

/**
 * Recognition audit log. The analytics-service reads its data from here.
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Recognition audit log")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Recent events", description = "Newest first, at most 200")
    public List<EventResponse> findRecent(
            @Parameter(description = "Number of rows to return (1..200)")
            @RequestParam(defaultValue = "20") int limit) {
        return eventService.findRecent(limit);
    }
}
