package dk.kb.stream.tests;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class WrappedResultSet implements AutoCloseable {
    protected final ResultSet resultSet;

    public WrappedResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public boolean isLast() {
        try {
            final boolean last = resultSet.isLast();
            System.err.println("wrs last=" + last);
            return last;
        } catch (SQLException e) {
            throw new RuntimeException("isLast()", e);
        }
    }

    public boolean next() {
        try {
            System.err.println("wrs next()");
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("next()", e);
        }
    }

    public String getString(String s) {
        try {
            System.err.println("wrs getString(" + s + ")");
            return resultSet.getString(s);
        } catch (SQLException e) {
            throw new RuntimeException("getString(" + s + ")", e);
        }
    }

    @Override
    public void close() {
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException("close()", e);
        }
    }
}
