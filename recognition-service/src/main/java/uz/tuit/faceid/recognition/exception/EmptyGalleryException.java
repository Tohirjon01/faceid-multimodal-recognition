package uz.tuit.faceid.recognition.exception;

/** The gallery holds no templates, so no demo probe can be derived. */
public class EmptyGalleryException extends RuntimeException {

    public EmptyGalleryException() {
        super("Gallery is empty — call POST /api/v1/demo/seed first");
    }
}
