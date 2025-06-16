package org.sagebionetworks;

public class DockerPullException extends RuntimeException {

    public DockerPullException() {
    }

    public DockerPullException(String message) {
        super(message);
    }

    public DockerPullException(Throwable cause) {
        super(cause);
    }

    public DockerPullException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerPullException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
