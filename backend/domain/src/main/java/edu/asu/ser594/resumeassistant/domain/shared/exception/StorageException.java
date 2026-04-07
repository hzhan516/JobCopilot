package edu.asu.ser594.resumeassistant.domain.shared.exception;

public class StorageException extends DomainException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
