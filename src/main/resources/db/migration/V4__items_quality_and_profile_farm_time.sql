CREATE TABLE profile_farm_time (
    farm_time_id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    farm_timeseconds BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (farm_time_id),
    INDEX idx_profile_farm_time_profile_id (profile_id),
    CONSTRAINT fk_profile_farm_time_profile
        FOREIGN KEY (profile_id) REFERENCES profile (profile_id)
        ON DELETE CASCADE
);
