package uz.tuit.faceid.recognition.exception;

import java.util.UUID;

/** No such subject exists in the registry. */
public class SubjectNotFoundException extends RuntimeException {

    public SubjectNotFoundException(UUID subjectId) {
        super("Subject not found: " + subjectId);
    }
}
