package by.bsuir.vstdio.dao.keys;

public enum LimiterType {
    MORE,
    MORE_OR_EQUALS,
    LESS,
    LESS_OR_EQUALS,
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN;

    @Override
    public String toString() {
        return switch (this) {
            case MORE -> ">";
            case MORE_OR_EQUALS -> ">=";
            case LESS -> "<";
            case LESS_OR_EQUALS -> "<=";
            case EQUALS -> "=";
            case NOT_EQUALS -> "<>";
            case IN -> "IN";
            case NOT_IN -> "NOT IN";
        };
    }
}
