-- Encrypted trade tokens are longer than the plain 8-character Steam token.
ALTER TABLE steamguard
    MODIFY trade_token VARCHAR(512) NULL;