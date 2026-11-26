package uz.tuit.faceid.recognition.mapper;

import org.springframework.stereotype.Component;
import uz.tuit.faceid.recognition.dto.response.EventResponse;
import uz.tuit.faceid.recognition.entity.RecognitionEvent;

import java.util.List;

@Component
public class EventMapper {

    /**
     * The probability distribution is deliberately left out of the response: it
     * is stored for research purposes, but seven numbers per row would be noise
     * in a log listing.
     */
    public EventResponse toResponse(RecognitionEvent event) {
        return new EventResponse(
                event.id(),
                event.occurredAt(),
                event.subjectId(),
                event.fullName(),
                event.cameraId(),
                event.decision(),
                event.matchSimilarity(),
                event.livenessScore(),
                event.expression(),
                event.modalitiesUsed());
    }

    public List<EventResponse> toResponseList(List<RecognitionEvent> events) {
        return events.stream().map(this::toResponse).toList();
    }
}
