package com.scasp.persistence;

import com.scasp.repository.DataAccessException;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the trusted, submitted SQL scripts (database/*.sql, packaged on the classpath under db/).
 * Statements end with ';' at end of line; lines starting with '--' are comments.
 * Only used for schema/sample-data set-up — never with user input.
 */
public final class SqlScriptRunner {
    private SqlScriptRunner() {}

    public static List<String> split(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : script.split("\\R")) {
            String t = line.strip();
            if (t.isEmpty() || t.startsWith("--")) continue;
            cur.append(line).append('\n');
            if (t.endsWith(";")) {
                String stmt = cur.toString().strip();
                out.add(stmt.substring(0, stmt.length() - 1));
                cur.setLength(0);
            }
        }
        if (!cur.toString().isBlank()) out.add(cur.toString().strip());
        return out;
    }

    public static int runClasspathScript(DataSource ds, String resource) {
        try (InputStream in = SqlScriptRunner.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("Script not found on classpath: " + resource);
            return run(ds, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + resource, e);
        }
    }

    public static int run(DataSource ds, String script) {
        List<String> statements = split(script);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            for (String sql : statements) s.execute(sql);
            return statements.size();
        } catch (SQLException e) {
            throw new DataAccessException("Script failed: " + e.getMessage(), e, e.getSQLState());
        }
    }

    /** Recreates the schema and loads the fictional sample data. */
    public static void resetDatabase(DataSource ds) {
        runClasspathScript(ds, "db/schema.sql");
        runClasspathScript(ds, "db/sample-data.sql");
    }
}
