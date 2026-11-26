package uz.tuit.faceid.identity.exception;

import java.util.UUID;

/** No such person exists in the registry. */
public class PersonNotFoundException extends RuntimeException {

    public PersonNotFoundException(UUID personId) {
        super("Person not found: " + personId);
    }
}
