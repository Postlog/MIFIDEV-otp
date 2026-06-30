package com.promoit.otp.dao;

import com.promoit.otp.config.DatabaseManager;
import com.promoit.otp.model.OtpCode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Plain-JDBC data access for the {@code otp_codes} table.
 *
 * <p>All time handling is performed on the database clock ({@code now()}): the
 * expiration is computed at insert time and every comparison uses the same
 * server clock, so there can be no JVM/DB timezone drift.
 */
public class OtpCodeDao {

    private final DatabaseManager db;

    public OtpCodeDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inserts a new code. The created/expires timestamps are computed by the
     * database from {@code now()} and the TTL, then read back onto the model.
     *
     * @param ttlSeconds time-to-live in seconds
     * @return generated id
     */
    public long insert(OtpCode code, long ttlSeconds) {
        String sql = "INSERT INTO otp_codes "
                + "(user_id, operation_id, code, status, channel, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, now(), now() + make_interval(secs => ?)) "
                + "RETURNING id, created_at, expires_at";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, code.getUserId());
            ps.setString(2, code.getOperationId());
            ps.setString(3, code.getCode());
            ps.setString(4, code.getStatus().name());
            ps.setString(5, code.getChannel() == null ? null : code.getChannel().name());
            ps.setLong(6, ttlSeconds);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong("id");
                code.setId(id);
                code.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                code.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                return id;
            }
        } catch (SQLException e) {
            throw new DataAccessException("insert otp code failed", e);
        }
    }

    /**
     * Atomically consumes a valid (ACTIVE, not expired) code for the user,
     * marking it USED in a single statement. This check-and-set is race-free:
     * concurrent attempts to redeem the same code cannot both succeed.
     *
     * <p>The operation binding is symmetric — a code issued for a specific
     * {@code operationId} can only be redeemed with that same id, and an
     * operation-less code only without one.
     *
     * @return the id of the consumed code, or empty if nothing matched
     */
    public Optional<Long> consumeActive(long userId, String code, String operationId) {
        String operationPredicate = (operationId != null)
                ? " AND operation_id = ?"
                : " AND operation_id IS NULL";
        String sql = "UPDATE otp_codes SET status = 'USED', used_at = now() "
                + "WHERE user_id = ? AND code = ? AND status = 'ACTIVE' AND expires_at > now()"
                + operationPredicate
                + " RETURNING id";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, code);
            if (operationId != null) {
                ps.setString(3, operationId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong("id")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("consumeActive failed", e);
        }
    }

    /**
     * Flips every ACTIVE code whose TTL has elapsed to EXPIRED, using the
     * database clock on both sides of the comparison.
     *
     * @return number of codes transitioned
     */
    public int markExpired() {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' "
                + "WHERE status = 'ACTIVE' AND expires_at < now()";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("markExpired failed", e);
        }
    }

    /** Deletes all OTP codes belonging to a user. Returns the number removed. */
    public int deleteByUserId(long userId) {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("deleteByUserId failed", e);
        }
    }

    /** Counts codes by user, used by the admin deletion summary. */
    public int countByUserId(long userId) {
        String sql = "SELECT count(*) FROM otp_codes WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("countByUserId failed", e);
        }
    }
}
