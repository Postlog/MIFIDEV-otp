package com.promoit.otp.dao;

import com.promoit.otp.config.DatabaseManager;
import com.promoit.otp.model.OtpConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Plain-JDBC data access for the single-row {@code otp_config} table.
 */
public class OtpConfigDao {

    private static final int CONFIG_ID = 1;

    private final DatabaseManager db;

    public OtpConfigDao(DatabaseManager db) {
        this.db = db;
    }

    public Optional<OtpConfig> find() {
        String sql = "SELECT id, code_length, ttl_seconds, updated_at FROM otp_config WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, CONFIG_ID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("find config failed", e);
        }
    }

    /** Inserts the single configuration row if it does not yet exist. */
    public void insertIfAbsent(int codeLength, long ttlSeconds) {
        String sql = "INSERT INTO otp_config (id, code_length, ttl_seconds) VALUES (?, ?, ?) "
                + "ON CONFLICT (id) DO NOTHING";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, CONFIG_ID);
            ps.setInt(2, codeLength);
            ps.setLong(3, ttlSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("insertIfAbsent config failed", e);
        }
    }

    /** Updates the configuration row and returns the new state. */
    public OtpConfig update(int codeLength, long ttlSeconds) {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ?, updated_at = now() "
                + "WHERE id = ? RETURNING id, code_length, ttl_seconds, updated_at";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setLong(2, ttlSeconds);
            ps.setInt(3, CONFIG_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DataAccessException("otp_config row is missing", null);
                }
                return map(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("update config failed", e);
        }
    }

    private OtpConfig map(ResultSet rs) throws SQLException {
        return new OtpConfig(
                rs.getInt("id"),
                rs.getInt("code_length"),
                rs.getLong("ttl_seconds"),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
