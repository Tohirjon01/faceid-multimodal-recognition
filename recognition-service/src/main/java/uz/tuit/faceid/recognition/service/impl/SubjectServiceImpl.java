package uz.tuit.faceid.recognition.service.impl;

import org.springframework.stereotype.Service;
import uz.tuit.faceid.recognition.dto.request.SubjectCreateRequest;
import uz.tuit.faceid.recognition.dto.response.SubjectResponse;
import uz.tuit.faceid.recognition.entity.Subject;
import uz.tuit.faceid.recognition.mapper.SubjectMapper;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.SubjectService;

import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {

    private final TemplateRepository templateRepository;
    private final SubjectMapper subjectMapper;

    public SubjectServiceImpl(TemplateRepository templateRepository, SubjectMapper subjectMapper) {
        this.templateRepository = templateRepository;
        this.subjectMapper = subjectMapper;
    }

    @Override
    public SubjectResponse create(SubjectCreateRequest request) {
        Subject subject = templateRepository.createSubject(request.fullName(), request.role());
        return subjectMapper.toResponse(subject, 0);
    }

    @Override
    public List<SubjectResponse> findAll() {
        return templateRepository.findAllSubjects().stream()
                .map(subject -> subjectMapper.toResponse(
                        subject, templateRepository.countTemplatesBySubject(subject.id())))
                .toList();
    }
}
