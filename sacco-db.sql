-- ============================================================
-- reset_all_sacco_dbs.sql
-- Drops and recreates every SACCO microservice database,
-- including the transactional outbox table in each service DB.
-- ⚠ Destroys all data. Use only in dev.
-- ============================================================

DROP DATABASE IF EXISTS sacco_member_db;
DROP DATABASE IF EXISTS sacco_account_db;
DROP DATABASE IF EXISTS sacco_loan_db;
DROP DATABASE IF EXISTS sacco_notification_db;


-- ============================================================
-- 1. MEMBER SERVICE DATABASE
-- ============================================================
CREATE DATABASE sacco_member_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sacco_member_db;

CREATE TABLE members (
                         id              CHAR(36)     NOT NULL DEFAULT (UUID()),
                         member_number   VARCHAR(50)  NOT NULL,
                         first_name      VARCHAR(100) NOT NULL,
                         last_name       VARCHAR(100) NOT NULL,
                         email           VARCHAR(255) NOT NULL,
                         phone_number    VARCHAR(20)  NOT NULL,
                         national_id     VARCHAR(50)  NOT NULL,
                         status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                         created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,
                         deleted_at      DATETIME     NULL     DEFAULT NULL,

                         PRIMARY KEY (id),
                         UNIQUE KEY uq_members_member_number (member_number),
                         UNIQUE KEY uq_members_email         (email),
                         UNIQUE KEY uq_members_phone_number  (phone_number),
                         UNIQUE KEY uq_members_national_id   (national_id),
                         KEY idx_members_status     (status),
                         KEY idx_members_deleted_at (deleted_at),
                         CONSTRAINT chk_members_status
                             CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB;

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
                               UNIQUE KEY uq_outbox_event_id (event_id),
                               KEY idx_outbox_unpublished (published_at, id),
                               KEY idx_outbox_aggregate   (aggregate_type, aggregate_id)
) ENGINE=InnoDB;


-- ============================================================
-- 2. SAVINGS / ACCOUNT SERVICE DATABASE
-- ============================================================
CREATE DATABASE sacco_account_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sacco_account_db;

CREATE TABLE accounts (
                          id              BIGINT        NOT NULL AUTO_INCREMENT,
                          account_number  VARCHAR(50)   NOT NULL,
                          member_id       CHAR(36)      NOT NULL,
                          balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                          account_type    VARCHAR(30)   NOT NULL,
                          status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                          created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,
                          deleted_at      DATETIME      NULL     DEFAULT NULL,

                          PRIMARY KEY (id),
                          UNIQUE KEY uq_accounts_account_number (account_number),
                          UNIQUE KEY uq_accounts_member_type    (member_id, account_type),
                          KEY idx_accounts_member_id  (member_id),
                          KEY idx_accounts_status     (status),
                          KEY idx_accounts_deleted_at (deleted_at),
                          CONSTRAINT chk_accounts_balance
                              CHECK (balance >= 0),
                          CONSTRAINT chk_accounts_type
                              CHECK (account_type IN ('SHARES', 'DEPOSITS', 'HOLIDAY_SAVINGS')),
                          CONSTRAINT chk_accounts_status
                              CHECK (status IN ('ACTIVE', 'FROZEN'))
) ENGINE=InnoDB;

CREATE TABLE transactions (
                              id                     BIGINT        NOT NULL AUTO_INCREMENT,
                              account_id             BIGINT        NOT NULL,
                              transaction_reference  VARCHAR(100)  NOT NULL,
                              amount                 DECIMAL(15,2) NOT NULL,
                              transaction_type       VARCHAR(20)   NOT NULL,
                              created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
                              deleted_at             DATETIME      NULL     DEFAULT NULL,

                              PRIMARY KEY (id),
                              UNIQUE KEY uq_transactions_reference (transaction_reference),
                              KEY idx_transactions_account_created (account_id, created_at),
                              KEY idx_transactions_type            (transaction_type),
                              KEY idx_transactions_deleted_at      (deleted_at),
                              CONSTRAINT fk_transactions_account
                                  FOREIGN KEY (account_id) REFERENCES accounts(id)
                                      ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT chk_transactions_amount
                                  CHECK (amount > 0),
                              CONSTRAINT chk_transactions_type
                                  CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL'))
) ENGINE=InnoDB;

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
                               UNIQUE KEY uq_outbox_event_id (event_id),
                               KEY idx_outbox_unpublished (published_at, id),
                               KEY idx_outbox_aggregate   (aggregate_type, aggregate_id)
) ENGINE=InnoDB;

CREATE TABLE processed_events (
                                  event_id      VARCHAR(100) NOT NULL,
                                  consumer      VARCHAR(100) NOT NULL,
                                  processed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (event_id, consumer),
                                  KEY idx_processed_at (processed_at)
) ENGINE=InnoDB;


-- ============================================================
-- 3. LOAN SERVICE DATABASE
-- ============================================================
CREATE DATABASE sacco_loan_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sacco_loan_db;

CREATE TABLE loan_applications (
                                   id                       BIGINT        NOT NULL AUTO_INCREMENT,
                                   member_id                CHAR(36)      NOT NULL,
                                   principal_amount         DECIMAL(15,2) NOT NULL,
                                   interest_rate            DECIMAL(5,2)  NOT NULL,
                                   repayment_period_months  INT           NOT NULL,
                                   status                   VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED',
                                   created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
                                   deleted_at               DATETIME      NULL     DEFAULT NULL,

                                   PRIMARY KEY (id),
                                   KEY idx_loan_applications_member_id  (member_id),
                                   KEY idx_loan_applications_status     (status),
                                   KEY idx_loan_applications_deleted_at (deleted_at),
                                   CONSTRAINT chk_loan_apps_principal
                                       CHECK (principal_amount > 0),
                                   CONSTRAINT chk_loan_apps_interest
                                       CHECK (interest_rate >= 0),
                                   CONSTRAINT chk_loan_apps_period
                                       CHECK (repayment_period_months > 0),
                                   CONSTRAINT chk_loan_apps_status
                                       CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'DISBURSED'))
) ENGINE=InnoDB;

CREATE TABLE active_loans (
                              id                     BIGINT        NOT NULL AUTO_INCREMENT,
                              loan_application_id    BIGINT        NOT NULL,
                              member_id              CHAR(36)      NOT NULL,
                              amount_disbursed       DECIMAL(15,2) NOT NULL,
                              remaining_balance      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                              next_payment_due_date  DATE          NULL,
                              status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                              created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
                              deleted_at             DATETIME      NULL     DEFAULT NULL,

                              PRIMARY KEY (id),
                              UNIQUE KEY uq_active_loans_application (loan_application_id),
                              KEY idx_active_loans_member_id        (member_id),
                              KEY idx_active_loans_status_due       (status, next_payment_due_date),
                              KEY idx_active_loans_deleted_at       (deleted_at),
                              CONSTRAINT fk_active_loans_application
                                  FOREIGN KEY (loan_application_id) REFERENCES loan_applications(id)
                                      ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT chk_active_loans_disbursed
                                  CHECK (amount_disbursed > 0),
                              CONSTRAINT chk_active_loans_balance
                                  CHECK (remaining_balance >= 0),
                              CONSTRAINT chk_active_loans_status
                                  CHECK (status IN ('ACTIVE', 'FULLY_PAID', 'DEFAULTED'))
) ENGINE=InnoDB;

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
                               UNIQUE KEY uq_outbox_event_id (event_id),
                               KEY idx_outbox_unpublished (published_at, id),
                               KEY idx_outbox_aggregate   (aggregate_type, aggregate_id)
) ENGINE=InnoDB;

CREATE TABLE processed_events (
                                  event_id      VARCHAR(100) NOT NULL,
                                  consumer      VARCHAR(100) NOT NULL,
                                  processed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (event_id, consumer),
                                  KEY idx_processed_at (processed_at)
) ENGINE=InnoDB;


-- ============================================================
-- 4. NOTIFICATION SERVICE DATABASE
-- ============================================================
CREATE DATABASE sacco_notification_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sacco_notification_db;

CREATE TABLE notification_logs (
                                   id                 BIGINT        NOT NULL AUTO_INCREMENT,
                                   member_id          CHAR(36)      NOT NULL,
                                   channel            VARCHAR(10)   NOT NULL,
                                   recipient_address  VARCHAR(255)  NOT NULL,
                                   message_body       TEXT          NOT NULL,
                                   status             VARCHAR(10)   NOT NULL,
                                   kafka_event_id     VARCHAR(100)  NOT NULL,
                                   sent_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
                                   deleted_at         DATETIME      NULL     DEFAULT NULL,

                                   PRIMARY KEY (id),
                                   UNIQUE KEY uq_notification_logs_kafka_event (kafka_event_id),
                                   KEY idx_notification_logs_member_id      (member_id),
                                   KEY idx_notification_logs_status_sent_at (status, sent_at),
                                   KEY idx_notification_logs_deleted_at     (deleted_at),
                                   CONSTRAINT chk_notification_logs_channel
                                       CHECK (channel IN ('SMS', 'EMAIL')),
                                   CONSTRAINT chk_notification_logs_status
                                       CHECK (status IN ('SENT', 'FAILED'))
) ENGINE=InnoDB;

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
                               UNIQUE KEY uq_outbox_event_id (event_id),
                               KEY idx_outbox_unpublished (published_at, id),
                               KEY idx_outbox_aggregate   (aggregate_type, aggregate_id)
) ENGINE=InnoDB;

CREATE TABLE processed_events (
                                  event_id      VARCHAR(100) NOT NULL,
                                  consumer      VARCHAR(100) NOT NULL,
                                  processed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (event_id, consumer),
                                  KEY idx_processed_at (processed_at)
) ENGINE=InnoDB;