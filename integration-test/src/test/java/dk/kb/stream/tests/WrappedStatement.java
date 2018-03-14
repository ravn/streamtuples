package dk.kb.stream.tests;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public class WrappedStatement implements AutoCloseable {
    private final Statement s;

    public WrappedStatement(Connection conn) {
        try {
            s = conn.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean execute(String sql) {
        try {
            return s.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public WrappedResultSet executeQuery(String sql) {
        try  {
            return new WrappedResultSet(s.executeQuery(sql));
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public void close() {
        try {
            s.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
