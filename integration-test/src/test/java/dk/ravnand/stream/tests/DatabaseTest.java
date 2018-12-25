package dk.ravnand.stream.tests;

import dk.ravnand.stream.StreamTuple;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @noinspection WeakerAccess, SqlNoDataSourceInspection
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
    public void pingDatabase() throws SQLException {
        try (var conn = Objects.requireNonNull(datasource, "datasource").getConnection()) {
            var statement = conn.prepareStatement("SELECT 1");
            assertTrue(statement.execute(), "execute()");
        }
    }

    /**
     * @noinspection Convert2MethodRef
     */
    @Test
    public void streamUpdateSimpleTable() throws SQLException {
        try (var conn = Objects.requireNonNull(datasource, "datasource").getConnection();
             var s = new WrappedStatement(conn)) {

            Stream.of("CREATE TABLE t (id INTEGER, s VARCHAR)",
                    "INSERT INTO t VALUES (3, 'Third')",
                    "INSERT INTO t VALUES (2, 'Second')",
                    "INSERT INTO t VALUES (1, 'First')",
                    "COMMIT")
                    .forEach(s::execute);

            // ---
            // Ask database for each id.

            var updateResult = Stream.of(2, 3)
                    .map(StreamTuple::create)
                    .map(st -> st.map(id -> {
                        final String sql = "SELECT * FROM t WHERE id=" + id;
                        try (ResultSet rs = s.executeQuery(sql)) {
                            rs.next();
                            return rs.getString("s");
                        } catch (SQLException e) {
                            throw new RuntimeException(sql, e);
                        }
                    }))
                    .map(st -> st.map(v -> ">" + v + "<"))
                    .map(st -> st.map((id, v) -> s.execute("UPDATE t SET s='" + v + "' WHERE id=" + id)))
                    .collect(toMap(st -> st.left(), st -> st.right()));

            var expectedUpdateResult = new HashMap<>();
            expectedUpdateResult.put(2, false);
            expectedUpdateResult.put(3, false);
            assertThat(updateResult, is(expectedUpdateResult));

            // ---  Do we have what we need.

            final var result = new HashMap<>();
            try (ResultSet rs = s.executeQuery("SELECT * from t")) {
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
