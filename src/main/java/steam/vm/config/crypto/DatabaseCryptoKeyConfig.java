package steam.vm.config.crypto;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class DatabaseCryptoKeyConfig {

    public DatabaseCryptoKeyConfig(Environment environment) {
        String configured = environment.getProperty("app.crypto.key-base64");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("APP_CRYPTO_KEY_BASE64");
        }
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Set app.crypto.key-base64 or APP_CRYPTO_KEY_BASE64");
        }

        DatabaseCrypto.setKey(Base64.getDecoder().decode(configured));
    }
}
