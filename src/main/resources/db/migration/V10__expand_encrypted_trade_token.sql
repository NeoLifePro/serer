-- trade_token is encrypted by EncryptedStringConverter, so it needs room for
-- the encryption prefix, IV, authentication tag and Base64 expansion.
ALTER TABLE steamguard
    MODIFY trade_token VARCHAR(1024) NULL;