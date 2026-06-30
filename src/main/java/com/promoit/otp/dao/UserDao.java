package com.promoit.otp.dao;

import com.promoit.otp.config.DatabaseManager;
import com.promoit.otp.model.Role;
import com.promoit.otp.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain-JDBC data access for the {@code users} table.
 */
public class UserDao {

    private final DatabaseManager db;

    public UserDao(DatabaseManager db) {
        this.db = db;
    }

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE login = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("findByLogin failed", e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("findById failed", e);
        }
    }

    public boolean existsByLogin(String login) {
        String sql = "SELECT 1 FROM users WHERE login = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("existsByLogin failed", e);
        }
    }

    public int countByRole(Role role) {
        String sql = "SELECT count(*) FROM users WHERE role = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("countByRole failed", e);
        }
    }

    /** Inserts a new user and returns the generated id. */
    public long insert(User user) {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateKeyException(e.getMessage(), e);
            }
            throw new DataAccessException("insert user failed", e);
        }
    }

    /** Returns all users whose role differs from the given one (e.g. all non-admins). */
    public List<User> findAllExcept(Role role) {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users "
                + "WHERE role <> ? ORDER BY id";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(map(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DataAccessException("findAllExcept failed", e);
        }
    }

    /** Deletes a user (OTP codes cascade away). Returns true if a row was removed. */
    public boolean deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("deleteById failed", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("login"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
