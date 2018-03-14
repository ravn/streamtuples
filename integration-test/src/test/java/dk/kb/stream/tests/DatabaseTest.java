package dk.kb.stream.tests;

import dk.kb.stream.StreamTuple;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @noinspection WeakerAccess
 */
public class DatabaseTest {

    protected DataSource datasource;

    @BeforeEach()
    public void setupDatabase() throws SQLException {
        JdbcDataSource ds;
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testDB");
        ds.setUser("sa");
        ds.setPassword("sa");
        datasource = ds;
    }

    @AfterEach
    public void destroyDatabase() throws SQLException {
        // don't know yet
    }

    @Test
    public void pingDatabase() throws SQLException {
        try (Connection conn = Objects.requireNonNull(datasource, "datasource").getConnection()) {
            PreparedStatement statement = conn.prepareStatement("SELECT 1");
            assertTrue(statement.execute(), "execute()");
        }
    }

    @Test
    public void streamUpdateSimpleTablePlainJDBC() throws SQLException {
        try (Connection conn = Objects.requireNonNull(datasource, "datasource").getConnection();
             WrappedStatement s = new WrappedStatement(conn)) {

            Stream.of("CREATE TABLE t (id INTEGER, s VARCHAR)",
                    "INSERT INTO t VALUES (3, 'Third')",
                    "INSERT INTO t VALUES (2, 'Second')",
                    "INSERT INTO t VALUES (1, 'First')",
                    "COMMIT")
                    .forEach(sql -> s.execute(sql));

            // ---
            // Ask database for each id.

            Map<Integer, Boolean> updateResult = Stream.of(2, 3)
                    .map(StreamTuple::create)
                    .map(st -> st.map(id -> {
                        final String sql = "SELECT * FROM t WHERE id=" + id;
                        try (WrappedResultSet rs = s.executeQuery(sql)) {
                            rs.next();
                            return rs.getString("s");
                        }
                    }))
                    .map(st -> st.map(v -> ">" + v + "<"))
                    .map(st -> st.map((id, v) -> s.execute("UPDATE t SET s='" + v + "' WHERE id=" + id)))
                    .collect(toMap(st -> st.left(), st -> st.right()));

            Map<Integer, Boolean> expectedUpdateResult = new HashMap<>();
            expectedUpdateResult.put(2, false);
            expectedUpdateResult.put(3, false);
            assertThat(updateResult, is(expectedUpdateResult));

            // ---  Do we have what we need.

            final Map<String, String> result = new HashMap<>();
            try (WrappedResultSet rs = s.executeQuery("SELECT * from t")) {
                while (rs.next()) {
                    result.put(rs.getString("id"), rs.getString("s"));
                }
            }

            Map<String, String> expectedResult = new TreeMap<>();
            expectedResult.put("1", "First");
            expectedResult.put("2", ">Second<");
            expectedResult.put("3", ">Third<");

            assertThat(result, is(expectedResult));
        }
    }

    @Test
    public void streamUpdateSimpleTableResultSetStreamIterate() throws SQLException {
        try (Connection conn = Objects.requireNonNull(datasource, "datasource").getConnection();
             WrappedStatement s1 = new WrappedStatement(conn);
             WrappedStatement s2 = new WrappedStatement(conn)) {

            Stream.of("CREATE TABLE t (id INTEGER, s VARCHAR)",
                    "INSERT INTO t VALUES (3, 'Third')",
                    "INSERT INTO t VALUES (2, 'Second')",
                    "INSERT INTO t VALUES (1, 'First')",
                    "COMMIT")
                    .forEach(sql -> s1.execute(sql));

            // ---
            // Ask database for each id.

            Map<Integer, Boolean> updateResult = Stream.of(2, 3)
                    .map(StreamTuple::create)
                    .flatMap(st -> st.flatMap(id -> {
                        final String sql = "SELECT * FROM t WHERE id=" + id;
                        System.err.println("sql: " + sql);
                        final WrappedResultSet wrappedResultSet = s1.executeQuery(sql);
                        if (!wrappedResultSet.isLast()) {
                            wrappedResultSet.next();
                        }
                        return Stream.iterate(wrappedResultSet,
                                rs -> {
                                    System.err.println("iterate hasNext");
                                    return !rs.isLast();
                                },
                                rs -> {
                                    System.err.println("iterate next");
                                    rs.next();
                                    System.err.println(rs.getString("id") + "= " + rs.getString("s"));
                                    return rs;
                                });
                    }))
                    .peek(t -> System.err.println("1 " + t.left() + ": " + t.right()))
                    .map(st -> st.map(rs -> rs.getString("s")))
                    .peek(t -> System.err.println("2 " + t.left() + ": " + t.right()))
                    .map(st -> st.map(v -> ">" + v + "<"))
                    .peek(t -> System.err.println("3 " + t.left() + ": " + t.right()))
                    .map(st -> st.map((id, v) -> s2.execute("UPDATE t SET s='" + v + "' WHERE id=" + id)))
                    .collect(toMap(st -> st.left(), st -> st.right()));

            Map<Integer, Boolean> expectedUpdateResult = new HashMap<>();
            expectedUpdateResult.put(2, false);
            expectedUpdateResult.put(3, false);
//            assertThat(updateResult, is(expectedUpdateResult));

            // ---  Do we have what we need.

            final Map<String, String> result = new HashMap<>();
            try (WrappedResultSet rs = s1.executeQuery("SELECT * from t")) {
                while (rs.next()) {
                    result.put(rs.getString("id"), rs.getString("s"));
                }
            }

            Map<String, String> expectedResult = new TreeMap<>();
            expectedResult.put("1", "First");
            expectedResult.put("2", ">Second<");
            expectedResult.put("3", ">Third<");

            assertThat(result, is(expectedResult));
        }
    }
}
