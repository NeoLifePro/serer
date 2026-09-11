package steam.vm.config.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

public final class DatabaseCrypto {
    private static final String PREFIX = "ENC:v1:";
    private static volatile byte[] key;

    private DatabaseCrypto() {
    }

    public static void setKey(byte[] rawKey) {
        if (rawKey == null || rawKey.length != 32) {
            throw new IllegalArgumentException("Database encryption key must be 32 bytes");
        }
        key = Arrays.copyOf(rawKey, rawKey.length);
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.startsWith(PREFIX)) {
            return plainText;
        }

        try {
            byte[] iv = deterministicIv(plainText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(), new GCMParameterSpec(128, iv));
            byte[] payload = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = new byte[iv.length + payload.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(payload, 0, encrypted, iv.length, payload.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt database value", e);
        }
    }

    public static String decrypt(String databaseValue) {
        if (databaseValue == null || !databaseValue.startsWith(PREFIX)) {
            return databaseValue;
        }

        try {
            byte[] encrypted = Base64.getUrlDecoder().decode(databaseValue.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = Arrays.copyOfRange(encrypted, 0, 12);
            byte[] payload = Arrays.copyOfRange(encrypted, 12, encrypted.length);
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt database value", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static byte[] deterministicIv(String plainText) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(requiredKey(), "HmacSHA256"));
        byte[] digest = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(digest, 12);
    }

    private static SecretKeySpec aesKey() {
        return new SecretKeySpec(requiredKey(), "AES");
    }

    private static byte[] requiredKey() {
        byte[] current = key;
        if (current == null) {
            throw new IllegalStateException("Database encryption key is not configured");
        }
        return current;
    }

}
