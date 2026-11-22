-- V1: core tables. Never edit this file after it has been applied;
-- put every later change in a new file (V2__..., V3__...).

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE job_applications (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company        VARCHAR(200)  NOT NULL,
    role_title     VARCHAR(200)  NOT NULL,
    location       VARCHAR(200),
    job_url        VARCHAR(1000),
    source         VARCHAR(100),
    status         VARCHAR(20)   NOT NULL DEFAULT 'APPLIED',
    applied_date   DATE,
    salary_min     NUMERIC(12, 2),
    salary_max     NUMERIC(12, 2),
    currency       VARCHAR(3),
    notes          TEXT,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_job_applications_status CHECK (
        status IN ('WISHLIST', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_job_applications_salary CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_max >= salary_min
    ),
    CONSTRAINT ck_job_applications_currency CHECK (
        currency IS NULL OR char_length(currency) = 3
    )
);

CREATE INDEX idx_job_applications_user_status ON job_applications (user_id, status);
CREATE INDEX idx_job_applications_user_applied_date ON job_applications (user_id, applied_date DESC);

CREATE TABLE status_history (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID         NOT NULL REFERENCES job_applications (id) ON DELETE CASCADE,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20)  NOT NULL,
    changed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_status_history_from CHECK (
        from_status IS NULL OR from_status IN
            ('WISHLIST', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_status_history_to CHECK (
        to_status IN ('WISHLIST', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')
    )
);

CREATE INDEX idx_status_history_application ON status_history (application_id);
