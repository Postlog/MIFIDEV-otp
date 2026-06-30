-- =====================================================================
--  MIFIDEV-otp database schema (PostgreSQL 17).
--  Executed automatically on application start-up (idempotent).
-- =====================================================================

-- ---- Users -----------------------------------------------------------
-- Stores login, the hashed password and the role (ADMIN | USER).
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    login         VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- Guarantees that at most ONE administrator can ever exist.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_single_admin
    ON users ((role)) WHERE role = 'ADMIN';

-- ---- OTP configuration ----------------------------------------------
-- Single-row table: the CHECK + fixed primary key make it impossible
-- to ever store more than one configuration record.
CREATE TABLE IF NOT EXISTS otp_config (
    id          INTEGER   PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    code_length INTEGER   NOT NULL CHECK (code_length BETWEEN 4 AND 10),
    ttl_seconds BIGINT    NOT NULL CHECK (ttl_seconds > 0),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- ---- OTP codes -------------------------------------------------------
-- One row per generated code, bound to a user and (optionally) an
-- operation identifier. Codes cascade-delete with their owner.
CREATE TABLE IF NOT EXISTS otp_codes (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    operation_id VARCHAR(128),
    code         VARCHAR(16)  NOT NULL,
    status       VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    channel      VARCHAR(16),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at   TIMESTAMP    NOT NULL,
    used_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_otp_codes_user   ON otp_codes (user_id);
CREATE INDEX IF NOT EXISTS idx_otp_codes_status ON otp_codes (status);
