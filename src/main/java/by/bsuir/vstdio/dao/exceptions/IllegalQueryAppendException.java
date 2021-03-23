package by.bsuir.vstdio.dao.exceptions;

public class IllegalQueryAppendException extends Exception{
    public IllegalQueryAppendException() {
        super();
    }

    public IllegalQueryAppendException(String message) {
        super(message);
    }

    public IllegalQueryAppendException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalQueryAppendException(Throwable cause) {
        super(cause);
    }
}
