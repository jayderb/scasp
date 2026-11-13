package com.scasp.repository;

/** Unchecked wrapper for persistence failures so callers do not depend on java.sql. */
public class DataAccessException extends RuntimeException {
    private final String sqlState;
    public DataAccessException(String message, Throwable cause, String sqlState) { super(message, cause); this.sqlState = sqlState; }
    public DataAccessException(String message, Throwable cause) { this(message, cause, null); }
    public String getSqlState() { return sqlState; }
    /** SQLState class 23 = integrity constraint violation (duplicate key, FK, CHECK, NOT NULL). */
    public boolean isConstraintViolation() { return sqlState != null && sqlState.startsWith("23"); }
}
