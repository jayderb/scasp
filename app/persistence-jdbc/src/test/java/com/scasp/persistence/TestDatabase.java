package com.scasp.persistence;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.Assumptions;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Integration tests run against a real MySQL 8 database supplied through environment variables
 * (never committed credentials):
 *   SCASP_TEST_JDBC_URL  e.g. jdbc:mysql://localhost:3306/scasp_test
 *   SCASP_TEST_DB_USER / SCASP_TEST_DB_PASSWORD
 * If SCASP_TEST_JDBC_URL is not set the tests are SKIPPED (reported as skipped, not passed).
 * WARNING: the tests drop and recreate every SCASP table in that database - use a dedicated test schema.
 */
final class TestDatabase {
    private TestDatabase() {}
    static DataSource dataSourceOrSkip() {
        String url = System.getenv("SCASP_TEST_JDBC_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "SCASP_TEST_JDBC_URL not set - MySQL integration tests skipped");
        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL(url);
        ds.setUser(System.getenv().getOrDefault("SCASP_TEST_DB_USER", "scasp"));
        ds.setPassword(System.getenv().getOrDefault("SCASP_TEST_DB_PASSWORD", ""));
        try (Connection c = ds.getConnection()) {
            System.out.println("[TestDatabase] " + c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            Assumptions.abort("Test database not reachable: " + e.getMessage());
        }
        return ds;
    }
}
