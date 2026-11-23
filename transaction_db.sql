-- ============================================================
-- TRANSACTION LEDGER SERVICE DATABASE
-- ============================================================
DROP DATABASE IF EXISTS sacco_transaction_db;

CREATE DATABASE sacco_transaction_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sacco_transaction_db;

-- 1. Chart of Accounts (SACCO GL + Member Account Mirrors)
CREATE TABLE ledger_accounts (
                                 id               BIGINT        NOT NULL AUTO_INCREMENT,
                                 account_number   VARCHAR(50)   NOT NULL,          -- Matches accounts.account_number or 'GL-1001'
                                 member_id        CHAR(36)      NULL,              -- NULL for internal SACCO GL accounts
                                 account_category VARCHAR(20)   NOT NULL,
                                 account_name     VARCHAR(120)  NOT NULL,
                                 current_balance  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                                 status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                                 created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
                                 deleted_at       DATETIME      NULL     DEFAULT NULL,

                                 PRIMARY KEY (id),
                                 UNIQUE KEY uq_ledger_accounts_number (account_number),
                                 KEY idx_ledger_accounts_member_id  (member_id),
                                 KEY idx_ledger_accounts_category   (account_category),
                                 KEY idx_ledger_accounts_deleted_at (deleted_at),
                                 CONSTRAINT chk_ledger_accounts_category
                                     CHECK (account_category IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
                                 CONSTRAINT chk_ledger_accounts_status
                                     CHECK (status IN ('ACTIVE', 'FROZEN'))
) ENGINE=InnoDB;

-- 2. Transactions Header (Idempotency + Dashboard Feed)
CREATE TABLE transactions (
                              id                     BIGINT        NOT NULL AUTO_INCREMENT,
                              transaction_uuid       CHAR(36)      NOT NULL DEFAULT (UUID()),
                              transaction_reference  VARCHAR(100)  NOT NULL,    -- M-Pesa receipt 'QK89L2M10P' or Cash Voucher 'CASH-HQ-001'
                              member_id              CHAR(36)      NULL,
                              account_number         VARCHAR(50)   NOT NULL,    -- Primary member account affected
                              transaction_type       VARCHAR(30)   NOT NULL,
                              payment_channel        VARCHAR(20)   NOT NULL,    -- Maps directly to GL-1001 (MPESA), GL-1002 (BANK), GL-1003 (CASH)
                              description            VARCHAR(255)  NOT NULL,    -- e.g. 'Monthly BOSA Deposit' or 'Cash Deposit at HQ'
                              amount                 DECIMAL(15,2) NOT NULL,
                              status                 VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
                              initiated_by           VARCHAR(64)   NOT NULL DEFAULT 'SYSTEM', -- Teller/Admin UUID for Cash, or 'SYSTEM_MPESA'
                              metadata               JSON          NULL,
                              created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
                              deleted_at             DATETIME      NULL     DEFAULT NULL,

                              PRIMARY KEY (id),
                              UNIQUE KEY uq_transactions_uuid      (transaction_uuid),
                              UNIQUE KEY uq_transactions_reference (transaction_reference),
                              KEY idx_transactions_member_created  (member_id, created_at),
                              KEY idx_transactions_account_created (account_number, created_at),
                              KEY idx_transactions_channel         (payment_channel),
                              KEY idx_transactions_status          (status),
                              KEY idx_transactions_deleted_at      (deleted_at),
                              CONSTRAINT chk_transactions_amount
                                  CHECK (amount > 0),
                              CONSTRAINT chk_transactions_type
                                  CHECK (transaction_type IN (
                                                              'DEPOSIT',
                                                              'WITHDRAWAL',
                                                              'DIVIDEND',
                                                              'FEE',
                                                              'REVERSAL'
                                      )),
                              CONSTRAINT chk_transactions_channel
                                  CHECK (payment_channel IN ('MPESA', 'CASH', 'BANK', 'INTERNAL')),
                              CONSTRAINT chk_transactions_status
                                  CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED'))
) ENGINE=InnoDB;

-- 3. Immutable Double-Entry Journal Lines
CREATE TABLE journal_entries (
                                 id                BIGINT        NOT NULL AUTO_INCREMENT,
                                 transaction_id    BIGINT        NOT NULL,
                                 ledger_account_id BIGINT        NOT NULL,
                                 entry_type        VARCHAR(10)   NOT NULL,
                                 amount            DECIMAL(15,2) NOT NULL,
                                 balance_after     DECIMAL(15,2) NOT NULL,
                                 created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 PRIMARY KEY (id),
                                 KEY idx_journal_transaction_id (transaction_id),
                                 KEY idx_journal_account_date   (ledger_account_id, created_at),
                                 CONSTRAINT fk_journal_transaction
                                     FOREIGN KEY (transaction_id) REFERENCES transactions(id)
                                         ON DELETE RESTRICT ON UPDATE CASCADE,
                                 CONSTRAINT fk_journal_account
                                     FOREIGN KEY (ledger_account_id) REFERENCES ledger_accounts(id)
                                         ON DELETE RESTRICT ON UPDATE CASCADE,
                                 CONSTRAINT chk_journal_entry_type
                                     CHECK (entry_type IN ('DEBIT', 'CREDIT')),
                                 CONSTRAINT chk_journal_amount
                                     CHECK (amount > 0)
) ENGINE=InnoDB;

-- 4. Kafka Transactional Outbox
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

-- 5. Kafka Consumer Idempotency Table
CREATE TABLE processed_events (
                                  event_id     CHAR(36)     NOT NULL,
                                  topic        VARCHAR(100) NOT NULL,
                                  processed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (event_id),
                                  KEY idx_processed_events_topic (topic, processed_at)
) ENGINE=InnoDB;

-- ============================================================
-- SEED CORE SACCO GENERAL LEDGER ACCOUNTS
-- ============================================================
INSERT INTO ledger_accounts (account_number, member_id, account_category, account_name) VALUES
                                                                                            ('GL-1001', NULL, 'ASSET',   'M-Pesa Paybill Float Account'),
                                                                                            ('GL-1002', NULL, 'ASSET',   'SACCO Main Bank Account'),
                                                                                            ('GL-1003', NULL, 'ASSET',   'HQ Cash Vault / Teller Till'),
                                                                                            ('GL-4001', NULL, 'REVENUE', 'Transaction & Withdrawal Fees');
