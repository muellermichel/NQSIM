package ch.ethz.systems.nqsim;

public final class ExceedingBufferException extends Exception {
    public ExceedingBufferException(String errorMessage) {
        super(errorMessage);
    }
}

