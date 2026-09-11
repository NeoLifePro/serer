CREATE TABLE IF NOT EXISTS auth_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_type VARCHAR(20) NOT NULL DEFAULT 'refresh',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_time DATETIME NOT NULL,
    last_used_time DATETIME DEFAULT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY token_hash (token_hash),
    KEY user_id (user_id),
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
