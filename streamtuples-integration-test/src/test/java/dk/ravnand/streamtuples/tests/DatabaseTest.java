package dk.ravnand.streamtuples.tests;

import dk.ravnand.streamtuples.StreamTuples;
import io.vavr.control.Try;
import org.apache.commons.dbutils.ResultSetIterator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @noinspection WeakerAccess, SqlNoDataSourceInspection, Convert2MethodRef
 */
public class DatabaseTest {

    protected DataSource datasource;

    @BeforeEach()
    public void setupDatabase() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testDB");
        ds.setUser("sa");
        ds.setPassword("sa");
        datasource = ds;
    }

    @AfterEach
    public void destroyDatabase() {
        // don't know yet
    }

    @Test
    public void isDatabaseWellBehaved() throws SQLException {
        try (var conn = Objects.requireNonNull(datasource, "datasource").getConnection();
             var statement = conn.prepareStatement("SELECT 1 AS ROW1");
             var resultSet = statement.executeQuery()
        ) {
            // Meta data
            assertEquals(1, resultSet.getMetaData().getColumnCount());
            assertEquals("ROW1", resultSet.getMetaData().getColumnName(1));

            // Cursor before first row
            assertTrue(resultSet.isBeforeFirst());
            assertFalse(resultSet.isLast());
            assertFalse(resultSet.isAfterLast());

            // First row
            assertEquals(true, resultSet.next());
            assertEquals(1, resultSet.getInt(1));

            // Cursor after first row, no more rows.
            assertFalse(resultSet.isBeforeFirst());
            assertTrue(resultSet.isLast());
            assertFalse(resultSet.isAfterLast());

            assertEquals(false, resultSet.next());

            // Cursor after last row.
            assertFalse(resultSet.isBeforeFirst());
            assertFalse(resultSet.isLast());
            assertTrue(resultSet.isAfterLast());
        }
    }

       @Test
    public void streamUpdateSimpleTable() throws SQLException {
        try (var conn = Objects.requireNonNull(datasource, "datasource").getConnection();
             var statement = conn.createStatement()) {

            Stream.of("CREATE TABLE t (id INTEGER, s VARCHAR)",
                    "INSERT INTO t VALUES (3, 'Third')",
                    "INSERT INTO t VALUES (2, 'Second')",
                    "INSERT INTO t VALUES (1, 'First')",
                    "COMMIT")
                    .forEach(s -> Try.of(() -> statement.execute(s)).get());

            // ---
            // Ask database for each id.  We use Try.of to tame some of the SQLExceptions.  This is work in progress.

            var updateResult = StreamTuples.streamOf(2, 3)
                    .map(t -> t.map(id -> { // ResultSets do not work well in streams. Better solution pending.
                        try (var resultSet = statement.executeQuery("SELECT * FROM t WHERE id=" + id)) {
                            // https://stackoverflow.com/a/24511534/53897
                            Object[] row = StreamSupport.stream(ResultSetIterator.iterable(resultSet).spliterator(), false)
                                    .findFirst()
                                    .get();
                            return row[1];
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .map(t -> t.map(v -> ">" + v + "<"))
                    .map(t -> t.map((id, v) -> Try.of(() -> statement.execute("UPDATE t SET s='" + v + "' WHERE id=" + id)).get()))
                    .collect(toMap(t -> t.left(), t -> t.right()));

            var expectedUpdateResult = Map.of(2, false, 3, false);
            assertThat(updateResult, is(expectedUpdateResult));

            // ---  Do we have what we need?

            final var result = new HashMap<>();
            try (ResultSet rs = statement.executeQuery("SELECT * from t")) {
                while (rs.next()) {
                    result.put(rs.getString("id"), rs.getString("s"));
                }
            }

            var expectedResult = new TreeMap<>();
            expectedResult.put("1", "First");
            expectedResult.put("2", ">Second<");
            expectedResult.put("3", ">Third<");
            assertThat(result, is(expectedResult));
        }
    }
}
