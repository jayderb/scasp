package com.scasp.persistence;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Developer tool: recreates the SCASP schema and loads the fictional sample data (same as running
 * database/schema.sql then database/sample-data.sql in the mysql client).
 * Usage: SCASP_DB_PASSWORD=... java -cp persistence.jar:core.jar:mysql-connector-j.jar
 *        com.scasp.persistence.DatabaseSetup jdbc:mysql://localhost:3306/scasp scasp
 * The password is read from the environment so it never appears in shell history or Git.
 */
public final class DatabaseSetup {
    private DatabaseSetup() {}
    public static void main(String[] args) {
        if (args.length < 2) { System.err.println("Usage: DatabaseSetup <jdbc-url> <user>   (password in SCASP_DB_PASSWORD)"); System.exit(1); }
        DataSource ds = new SimpleDataSource(args[0], args[1], System.getenv().getOrDefault("SCASP_DB_PASSWORD", ""));
        SqlScriptRunner.resetDatabase(ds);
        System.out.println("Schema created and sample data loaded into " + args[0]);
    }
    /** Minimal non-pooled DataSource for this command-line tool only (the application server provides the pool). */
    record SimpleDataSource(String url, String user, String password) implements DataSource {
        public Connection getConnection() throws SQLException { return DriverManager.getConnection(url, user, password); }
        public Connection getConnection(String u, String p) throws SQLException { return DriverManager.getConnection(url, u, p); }
        public PrintWriter getLogWriter() { return null; }
        public void setLogWriter(PrintWriter out) { }
        public void setLoginTimeout(int seconds) { DriverManager.setLoginTimeout(seconds); }
        public int getLoginTimeout() { return DriverManager.getLoginTimeout(); }
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
        public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public String toString() { return "SimpleDataSource[" + url + ", " + user + "]"; }
    }
}
