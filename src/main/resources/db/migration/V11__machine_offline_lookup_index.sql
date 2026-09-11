-- Speeds up the one-second offline check and narrows the rows MySQL must inspect/lock.
CREATE INDEX idx_computer_status_last_seen_id
    ON monitored_computer (status, last_seen, computer_id);