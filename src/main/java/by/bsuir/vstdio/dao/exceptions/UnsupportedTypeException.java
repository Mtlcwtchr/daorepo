package by.bsuir.vstdio.dao.exceptions;

public class UnsupportedTypeException extends RuntimeException {
    public UnsupportedTypeException() {
        super();
    }

    public UnsupportedTypeException(String message) {
        super(message);
    }

    public UnsupportedTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedTypeException(Throwable cause) {
        super(cause);
    }
}
