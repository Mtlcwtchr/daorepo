package by.bsuir.vstdio.dao.keys;

public enum AggregatorFunc {
    SUM,
    COUNT,
    MAX,
    MIN,
    AVG;

    public String of(String column) {
        return switch (this) {
            case SUM -> "SUM(" + column + ")";
            case COUNT -> "COUNT(" + column + ")";
            case MAX -> "MAX(" + column + ")";
            case MIN -> "MIN(" + column + ")";
            case AVG -> "AVG(" + column + ")";
        };
    }
}
