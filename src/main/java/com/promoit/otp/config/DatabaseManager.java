package com.promoit.otp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Owns the JDBC {@link DataSource} (a HikariCP pool) and bootstraps the schema.
 * All DAO interaction goes through plain JDBC connections handed out here.
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final HikariDataSource dataSource;

    public DatabaseManager(AppConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.get("db.url"));
        hikari.setUsername(config.get("db.user"));
        hikari.setPassword(config.get("db.password"));
        hikari.setMaximumPoolSize(config.getInt("db.pool.size", 10));
        hikari.setPoolName("otp-pool");
        hikari.setConnectionTimeout(10_000);
        this.dataSource = new HikariDataSource(hikari);
        log.info("Initialised JDBC connection pool for {}", config.get("db.url"));
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Creates the tables (idempotently) from {@code schema.sql} on the classpath.
     */
    public void initSchema() {
        String script = readResource("schema.sql");
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String statement : script.split(";")) {
                String sql = statement.strip();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
            log.info("Database schema initialised");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialise database schema", e);
        }
    }

    private String readResource(String name) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + name);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read resource: " + name, e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("JDBC connection pool closed");
        }
    }
}
