package dev.mainul35.cms.sitebuilder.publish;

/** A site cannot be published as it stands. The message is meant for the person publishing. */
public class PublishException extends RuntimeException {
    public PublishException(String message) {
        super(message);
    }

    public PublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
