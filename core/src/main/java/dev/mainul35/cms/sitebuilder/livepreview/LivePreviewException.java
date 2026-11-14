package dev.mainul35.cms.sitebuilder.livepreview;

/** The live preview could not be started. The message is meant for the designer. */
public class LivePreviewException extends RuntimeException {

    public LivePreviewException(String message) {
        super(message);
    }

    public LivePreviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
