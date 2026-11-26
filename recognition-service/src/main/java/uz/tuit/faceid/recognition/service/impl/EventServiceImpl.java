package uz.tuit.faceid.recognition.service.impl;

import org.springframework.stereotype.Service;
import uz.tuit.faceid.recognition.dto.response.EventResponse;
import uz.tuit.faceid.recognition.mapper.EventMapper;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.EventService;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 200;

    private final TemplateRepository templateRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(TemplateRepository templateRepository, EventMapper eventMapper) {
        this.templateRepository = templateRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    public List<EventResponse> findRecent(int limit) {
        int safeLimit = Math.clamp(limit, MIN_LIMIT, MAX_LIMIT);
        return eventMapper.toResponseList(templateRepository.findRecentEvents(safeLimit));
    }
}
