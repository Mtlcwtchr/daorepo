package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.exceptions.IllegalQueryAppendException;
import by.bsuir.vstdio.dao.keys.LimiterConjunctionType;
import by.bsuir.vstdio.dao.keys.LimiterType;

public class QueryBuilder {

    private final StringBuilder query;


    private QueryBuilder() {
        this.query = new StringBuilder();
    }


    public static QueryBuilder select(String tableName, String... columns) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.query.append("SELECT ");
        for(int i = 0; i < columns.length; ++i) {
            queryBuilder.query.append(columns[i]).append(i < (columns.length - 1) ? ", " : " ");
        }
        queryBuilder.query.append("FROM ").append(tableName).append(" ");
        return queryBuilder;
    }

    public static QueryBuilder insert(String tableName, String... columns) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.query.append("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < columns.length; ++i) {
            queryBuilder.query.append(columns[i]).append(i < (columns.length - 1) ? ", " : ") ");
        }
        queryBuilder.query.append("VALUES(");
        for (int i = 0; i < columns.length; ++i) {
            queryBuilder.query.append(i < (columns.length - 1) ? "?, " : "?) ");
        }
        return queryBuilder;
    }

    public static QueryBuilder update(String tableName, String... columns) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.query.append("UPDATE ").append(tableName).append(" SET ");
        for (int i = 0; i < columns.length; ++i) {
            queryBuilder.query.append(columns[i]).append("=?").append(i < (columns.length - 1) ? ", " : " ");
        }
        return queryBuilder;
    }

    public static QueryBuilder delete(String tableName) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.query.append("DELETE FROM ").append(tableName).append(" ");
        return queryBuilder;
    }

    public QueryBuilder join(String joiningTableName, String foreignKeyName, String referenceTableKeyName) {
        query.append("JOIN ").append(joiningTableName).append(" ON ").append(foreignKeyName).append(LimiterType.EQUALS).append(referenceTableKeyName).append(" ");
        return this;
    }

    public QueryBuilder where(String columnName, LimiterType limiterType) throws IllegalQueryAppendException {
        if(query.toString().contains("GROUP BY")) {
            throw new IllegalQueryAppendException("Can't append WHERE to group-by query, use HAVING instead");
        }
        if(query.toString().contains("WHERE")) {
            throw new IllegalQueryAppendException("Limiters conjunction required");
        } else {
            query.append("WHERE ").append(columnName).append(limiterType).append("? ");
            return this;
        }
    }

    public QueryBuilder where(String columnName, LimiterType limiterType, LimiterConjunctionType limiterConjunctionType) throws IllegalQueryAppendException {
        if(query.toString().contains("GROUP BY")) {
            throw new IllegalQueryAppendException("Can't append WHERE to group-by query, use HAVING instead");
        }
        if(query.toString().contains("WHERE")) {
            query.append(limiterConjunctionType).append(" ").append(columnName).append(limiterType).append("? ");
        } else {
            query.append("WHERE ").append(columnName).append(limiterType).append("? ");
        }
        return this;
    }

    public QueryBuilder having(String columnName, LimiterType limiterType) throws IllegalQueryAppendException {
        if(query.toString().contains("GROUP BY")) {
            throw new IllegalQueryAppendException("Can't append HAVING to non group-by query, use WHERE instead");
        }
        if(query.toString().contains("HAVING")) {
            throw new IllegalQueryAppendException("Limiters conjunction required");
        } else {
            query.append("HAVING ").append(columnName).append(limiterType).append("? ");
            return this;
        }
    }

    public QueryBuilder having(String columnName, LimiterType limiterType, LimiterConjunctionType limiterConjunctionType) throws IllegalQueryAppendException {
        if(query.toString().contains("GROUP BY")) {
            throw new IllegalQueryAppendException("Can't append HAVING to non group-by query, use WHERE instead");
        }
        if(query.toString().contains("HAVING")) {
            query.append(limiterConjunctionType).append(" ").append(columnName).append(limiterType).append("? ");
        } else {
            query.append("HAVING ").append(columnName).append(limiterType).append("? ");
        }
        return this;
    }

    public QueryBuilder orderBy(String columnName, boolean isDescending) {
        query.append("ORDER BY ").append(columnName).append(isDescending ? "DESC " : "ASC ");
        return this;
    }

    public QueryBuilder orderBy(String columnName) {
        return orderBy(columnName, false);
    }

    public QueryBuilder limit(int countOfView, int offset) {
        query.append("LIMIT ").append(countOfView).append(" OFFSET ").append(offset).append(" ");
        return this;
    }

    public QueryBuilder limit(int countOfView) {
        return limit(countOfView, 0);
    }

    public QueryBuilder groupBy(String columnName) {
        query.append("GROUP BY ").append(columnName).append(" ");
        return this;
    }

    public String getQuery() {
        return query.toString();
    }
}
