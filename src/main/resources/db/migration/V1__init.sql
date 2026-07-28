CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255)              NOT NULL UNIQUE,
    password   VARCHAR(255)              NOT NULL,
    full_name  VARCHAR(255)              NOT NULL,
    role       SMALLINT                  NOT NULL CHECK (role BETWEEN 0 AND 1),
    is_active  BOOLEAN                   NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens
(
    id           UUID PRIMARY KEY,
    user_id      UUID                        NOT NULL REFERENCES users (id),
    hashed_token VARCHAR(255)                NOT NULL,
    expires_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hashed_token ON refresh_tokens (hashed_token);
