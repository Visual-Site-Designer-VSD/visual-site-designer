package dev.mainul35.vsd.runtime.auth.sso;

/** Sign-in through the auth server failed. The message is for the log, never for the visitor. */
public class SsoException extends RuntimeException {

    public SsoException(String message) {
        super(message);
    }

    public SsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
