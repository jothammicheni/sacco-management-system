-- ============================================================
-- sacco_auth_db.sql
-- Auth Service database schema.
-- ⚠ Destroys all data if run with DROP. Use only in dev.
-- ============================================================

DROP DATABASE IF EXISTS sacco_auth_db;

CREATE DATABASE sacco_auth_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sacco_auth_db;

-- ============================================================
-- USERS
-- One row per member (or staff member) who can log in.
-- ============================================================
CREATE TABLE users (
                       id                    CHAR(36)     NOT NULL,              -- same as memberId
                       phone_number          VARCHAR(20)  NOT NULL,              -- primary login identifier
                       email                 VARCHAR(255) NULL,                  -- optional
                       national_id           VARCHAR(20)  NULL,                  -- KYC reference

                       password_hash         VARCHAR(255) NULL,                  -- bcrypt, NULL until activated
                       pin_hash              VARCHAR(255) NULL,                  -- bcrypt hash of 6-digit PIN

                       role                  VARCHAR(30)  NOT NULL DEFAULT 'CUSTOMER',
                       status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

                       login_attempts        INT          NOT NULL DEFAULT 0,
                       login_locked_until    DATETIME     NULL     DEFAULT NULL,

                       pin_attempts          INT          NOT NULL DEFAULT 0,
                       pin_locked_until      DATETIME     NULL     DEFAULT NULL,

                       created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at            DATETIME     NULL     DEFAULT NULL,

                       PRIMARY KEY (id),
                       UNIQUE KEY uq_users_phone (phone_number),
                       UNIQUE KEY uq_users_email (email),
                       KEY idx_users_status (status),

                       CONSTRAINT chk_users_role
                           CHECK (role IN ('CUSTOMER', 'TELLER', 'ADMIN')),
                       CONSTRAINT chk_users_status
                           CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED'))
) ENGINE=InnoDB;


-- ============================================================
-- ACTIVATION TOKENS
-- One-time tokens for account activation (self-registration flow).
-- ============================================================
CREATE TABLE activation_tokens (
                                   token         CHAR(36)     NOT NULL,
                                   user_id       CHAR(36)     NOT NULL,
                                   expires_at    DATETIME     NOT NULL,
                                   used_at       DATETIME     NULL     DEFAULT NULL,
                                   created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   PRIMARY KEY (token),
                                   KEY idx_activation_user    (user_id),
                                   KEY idx_activation_expires (expires_at)
) ENGINE=InnoDB;


-- ============================================================
-- REFRESH TOKENS
-- Long-lived tokens to obtain new access tokens without re-login.
-- ============================================================
CREATE TABLE refresh_tokens (
                                token         CHAR(36)     NOT NULL,
                                user_id       CHAR(36)     NOT NULL,
                                expires_at    DATETIME     NOT NULL,
                                revoked_at    DATETIME     NULL     DEFAULT NULL,
                                created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                PRIMARY KEY (token),
                                KEY idx_refresh_user (user_id)
) ENGINE=InnoDB;


-- ============================================================
-- OTP CODES
-- One-time codes for activation, password reset, PIN reset.
-- Stores hashes, not plaintext.
-- ============================================================
CREATE TABLE otp_codes (
                           id           BIGINT       NOT NULL AUTO_INCREMENT,
                           user_id      CHAR(36)     NOT NULL,
                           code_hash    VARCHAR(255) NOT NULL,
                           purpose      VARCHAR(30)  NOT NULL,
                           expires_at   DATETIME     NOT NULL,
                           used_at      DATETIME     NULL     DEFAULT NULL,
                           attempts     INT          NOT NULL DEFAULT 0,
                           created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           PRIMARY KEY (id),
                           KEY idx_otp_user_purpose (user_id, purpose),
                           KEY idx_otp_expires      (expires_at),
                           CONSTRAINT chk_otp_purpose
                               CHECK (purpose IN ('ACTIVATION', 'PASSWORD_RESET', 'PIN_RESET'))
) ENGINE=InnoDB;


-- ============================================================
-- OUTBOX EVENTS
-- Transactional outbox for publishing auth-related events.
-- ============================================================
CREATE TABLE outbox_events (
                               id              BIGINT       NOT NULL AUTO_INCREMENT,
                               event_id        CHAR(36)     NOT NULL,
                               aggregate_type  VARCHAR(50)  NOT NULL,
                               aggregate_id    VARCHAR(100) NOT NULL,
                               event_type      VARCHAR(100) NOT NULL,
                               topic           VARCHAR(100) NOT NULL,
                               kafka_key       VARCHAR(100) NOT NULL,
                               payload         JSON         NOT NULL,
                               created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at    DATETIME     NULL     DEFAULT NULL,
                               attempts        INT          NOT NULL DEFAULT 0,
                               last_error      TEXT         NULL,

                               PRIMARY KEY (id),
                               UNIQUE KEY uq_outbox_event_id   (event_id),
                               KEY idx_outbox_unpublished      (published_at, id),
                               KEY idx_outbox_aggregate        (aggregate_type, aggregate_id)
) ENGINE=InnoDB;


-- ============================================================
-- PROCESSED EVENTS (inbox)
-- Consumer-side deduplication for member.created and other events.
-- ============================================================
CREATE TABLE processed_events (
                                  event_id      VARCHAR(100) NOT NULL,
                                  consumer      VARCHAR(100) NOT NULL,
                                  processed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (event_id, consumer),
                                  KEY idx_processed_at (processed_at)
) ENGINE=InnoDB;