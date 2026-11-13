package com.scasp.persistence;

import com.scasp.repository.DataAccessException;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared JDBC plumbing. Every resource is opened in try-with-resources; every user-controlled value is bound
 * through a PreparedStatement parameter (never concatenated into SQL), which prevents SQL injection because
 * the driver sends the value as data, not as SQL text.
 */
abstract class JdbcSupport {
    protected final DataSource dataSource;
    /** true when a Jakarta EE container (JTA) owns the transaction, e.g. inside a CMT enterprise bean. */
    private final boolean containerManaged;
    protected JdbcSupport(DataSource dataSource) { this(dataSource, false); }
    protected JdbcSupport(DataSource dataSource, boolean containerManaged) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.containerManaged = containerManaged;
    }

    @FunctionalInterface interface SqlWork<T> { T run(Connection c) throws SQLException; }
    @FunctionalInterface interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    /**
     * Runs work in one transaction: commit on success, rollback on ANY exception (SQL or business rule),
     * then restore auto-commit before the pooled connection is returned.
     */
    protected <T> T inTransaction(SqlWork<T> work) {
        if (containerManaged) {
            // Under JTA the container commits/rolls back; calling commit()/setAutoCommit() here is illegal.
            // Any exception propagates so the enterprise bean marks the transaction for rollback.
            return withConnection(work);
        }
        try (Connection c = dataSource.getConnection()) {
            boolean oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    protected <T> T withConnection(SqlWork<T> work) {
        try (Connection c = dataSource.getConnection()) { return work.run(c); }
        catch (SQLException e) { throw translate(e); }
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return withConnection(c -> query(c, sql, mapper, params));
    }
    protected static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }
    protected static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, params); return ps.executeUpdate(); }
    }
    /** Executes an INSERT and returns the AUTO_INCREMENT key via getGeneratedKeys. */
    protected static long insertReturningKey(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned");
                return keys.getLong(1);
            }
        }
    }
    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) ps.setNull(i + 1, Types.VARCHAR);
            else if (p instanceof String s) ps.setString(i + 1, s);
            else if (p instanceof Integer n) ps.setInt(i + 1, n);
            else if (p instanceof Long n) ps.setLong(i + 1, n);
            else if (p instanceof Boolean b) ps.setBoolean(i + 1, b);
            else ps.setObject(i + 1, p);
        }
    }
    /** Audit row written on the caller's connection so it commits or rolls back with the business change. */
    protected static void audit(Connection c, String actor, String action, String entityType, String entityId, String details) throws SQLException {
        update(c, "INSERT INTO audit_event (actor, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)",
                actor, action, entityType, entityId, details == null ? "" : details);
    }
    protected static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
    protected static DataAccessException translate(SQLException e) {
        return new DataAccessException("Database operation failed: " + e.getMessage(), e, e.getSQLState());
    }
}
