package uz.tuit.faceid.recognition.service;

import uz.tuit.faceid.recognition.dto.request.SubjectCreateRequest;
import uz.tuit.faceid.recognition.dto.response.SubjectResponse;

import java.util.List;

/** Subject registry. */
public interface SubjectService {

    SubjectResponse create(SubjectCreateRequest request);

    List<SubjectResponse> findAll();
}
