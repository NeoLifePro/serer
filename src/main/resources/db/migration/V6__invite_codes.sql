CREATE TABLE invite_codes (
  id bigint unsigned NOT NULL AUTO_INCREMENT,
  code varchar(64) NOT NULL,
  created_by_user_id bigint unsigned NOT NULL,
  activated_by_user_id bigint unsigned DEFAULT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  activated_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_invite_code (code),
  KEY idx_invite_created_by (created_by_user_id),
  KEY idx_invite_activated_by (activated_by_user_id),
  CONSTRAINT fk_invite_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_invite_activated_by
    FOREIGN KEY (activated_by_user_id) REFERENCES users (id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
