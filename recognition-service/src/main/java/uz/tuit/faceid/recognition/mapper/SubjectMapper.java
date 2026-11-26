package uz.tuit.faceid.recognition.mapper;

import org.springframework.stereotype.Component;
import uz.tuit.faceid.recognition.dto.response.SubjectResponse;
import uz.tuit.faceid.recognition.entity.Subject;

@Component
public class SubjectMapper {

    public SubjectResponse toResponse(Subject subject, int templateCount) {
        return new SubjectResponse(
                subject.id(),
                subject.fullName(),
                subject.role(),
                subject.consentGivenAt(),
                templateCount);
    }
}
