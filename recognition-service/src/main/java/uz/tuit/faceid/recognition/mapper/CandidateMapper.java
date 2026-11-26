package uz.tuit.faceid.recognition.mapper;

import org.springframework.stereotype.Component;
import uz.tuit.faceid.recognition.dto.response.CandidateResponse;
import uz.tuit.faceid.recognition.entity.MatchCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts candidates to their response shape and assigns the rank.
 * <p>
 * The rank is computed here rather than in the service: it exists purely for
 * presentation and has no bearing on the matching logic.
 */
@Component
public class CandidateMapper {

    public List<CandidateResponse> toResponseList(List<MatchCandidate> candidates, double threshold) {
        List<CandidateResponse> responses = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            MatchCandidate candidate = candidates.get(index);
            responses.add(new CandidateResponse(
                    index + 1,
                    candidate.subjectId(),
                    candidate.fullName(),
                    candidate.similarity(),
                    candidate.similarity() >= threshold));
        }
        return responses;
    }
}
