package edu.asu.ser594.resumeassistant.domain.shared.exception;

public class StorageException extends DomainException {
    public StorageException(String messageKey) {
        super(messageKey);
    }

    public StorageException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
