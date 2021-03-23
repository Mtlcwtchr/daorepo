package by.bsuir.vstdio.dao.keys;

public enum LimiterConjunctionType {
    AND,
    OR,
    NOT;

    @Override
    public String toString() {
        return switch (this) {
            case AND -> "AND";
            case OR -> "OR";
            case NOT -> "NOT";
        };
    }
}
