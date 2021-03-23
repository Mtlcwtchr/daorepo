import by.bsuir.vstdio.dao.exceptions.IllegalQueryAppendException;
import by.bsuir.vstdio.dao.keys.LimiterConjunctionType;
import by.bsuir.vstdio.dao.keys.LimiterType;
import by.bsuir.vstdio.dao.QueryBuilder;
import org.junit.Assert;
import org.junit.Test;

public class QueryBuilderTest {

    @Test
    public void QueryBuilderTest_SimpleSelectTestQuery() {
        String expectedQuery = "SELECT col1, col2, col3 FROM tableName ";
        String actualQuery =
                QueryBuilder
                .select("tableName", "col1", "col2", "col3")
                        .getQuery();
        Assert.assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void QueryBuilderTest_SimpleInsertTestQuery() {
        String expectedQuery = "INSERT INTO tableName (col1, col2, col3) VALUES(?, ?, ?) ";
        String actualQuery =
                QueryBuilder
                .insert("tableName", "col1", "col2", "col3")
                .getQuery();
        Assert.assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void QueryBuilderTest_SimpleUpdateTestQuery() {
        String expectedQuery = "UPDATE tableName SET col1=?, col2=?, col3=? ";
        String actualQuery =
                QueryBuilder
                .update("tableName", "col1", "col2", "col3")
                        .getQuery();
        Assert.assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void QueryBuilderTest_SimpleDeleteTestQuery() {
        String expectedQuery = "DELETE FROM tableName";
        String actualQuery =
                QueryBuilder
                .delete("tableName")
                .getQuery();
        Assert.assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void QueryBuilderTest_SelectWithLimiterTestQuery() throws IllegalQueryAppendException{
        String expectedQuery = "SELECT col1, col2, col3 FROM tableName WHERE col1=? AND col2=? ";
        String actualQuery =
                QueryBuilder
                .select("tableName", "col1", "col2", "col3")
                .where("col1", LimiterType.EQUALS)
                .where("col2", LimiterType.EQUALS, LimiterConjunctionType.AND)
                .getQuery();
        Assert.assertEquals(expectedQuery, actualQuery);
    }

    @Test(expected = IllegalQueryAppendException.class)
    public void QueryBuilderTest_SelectWithLimiterTestQueryExpectedException() throws IllegalQueryAppendException {
        String expectedQuery = "SELECT col1, col2, col3 FROM tableName WHERE col1=? AND col2=? ";
        String actualQuery =
                QueryBuilder
                        .select("tableName", "col1", "col2", "col3")
                        .where("col1", LimiterType.EQUALS)
                        .where("col2", LimiterType.EQUALS)
                        .getQuery();
        Assert.assertEquals(expectedQuery, actualQuery);
    }

}
