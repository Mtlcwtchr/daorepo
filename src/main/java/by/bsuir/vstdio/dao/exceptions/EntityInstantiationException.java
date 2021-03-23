package by.bsuir.vstdio.dao.exceptions;

public class EntityInstantiationException extends RuntimeException{
    public EntityInstantiationException() {
        super();
    }

    public EntityInstantiationException(String message) {
        super(message);
    }

    public EntityInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityInstantiationException(Throwable cause) {
        super(cause);
    }
}
