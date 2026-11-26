package uz.tuit.faceid.recognition.service;

import uz.tuit.faceid.recognition.dto.response.EventResponse;

import java.util.List;

/** Recognition audit log. */
public interface EventService {

    /** Most recent events first. */
    List<EventResponse> findRecent(int limit);
}
