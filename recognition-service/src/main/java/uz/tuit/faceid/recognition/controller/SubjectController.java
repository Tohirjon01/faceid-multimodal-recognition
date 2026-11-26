package uz.tuit.faceid.recognition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.recognition.dto.request.SubjectCreateRequest;
import uz.tuit.faceid.recognition.dto.response.SubjectResponse;
import uz.tuit.faceid.recognition.service.SubjectService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subjects", description = "Subject registry")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a subject")
    public SubjectResponse create(@Valid @RequestBody SubjectCreateRequest request) {
        return subjectService.create(request);
    }

    @GetMapping
    @Operation(summary = "List all subjects", description = "Sorted by name")
    public List<SubjectResponse> findAll() {
        return subjectService.findAll();
    }
}
