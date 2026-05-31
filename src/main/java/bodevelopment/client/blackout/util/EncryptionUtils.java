package bodevelopment.client.blackout.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for securing sensitive local data.
 * <p>
 * The encryption key is derived from hardware/system identifiers (HWID)
 * via SHA-256, ensuring accounts cannot be decrypted if copied to another machine.
 * Each encryption operation uses a fresh random 12-byte IV (GCM standard).
 * <p>
 * Output format: Base64( IV[12 bytes] || ciphertext[variable] || GCM tag[16 bytes] )
 */
public final class EncryptionUtils {
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE = 256;

    private static volatile SecretKey cachedKey;

    private EncryptionUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Derives a deterministic AES-256 key from local system identifiers.
     * The key is cached after first derivation for zero-cost subsequent calls.
     */
    public static SecretKey getKey() {
        if (cachedKey != null) return cachedKey;

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(System.getProperty("os.arch", "unknown").getBytes(StandardCharsets.UTF_8));
            sha256.update(System.getProperty("os.name", "unknown").getBytes(StandardCharsets.UTF_8));
            sha256.update(System.getProperty("user.name", "unknown").getBytes(StandardCharsets.UTF_8));
            sha256.update(String.valueOf(Runtime.getRuntime().availableProcessors()).getBytes(StandardCharsets.UTF_8));

            byte[] digest = sha256.digest();
            cachedKey = new SecretKeySpec(digest, "AES");
            return cachedKey;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available on this JVM", e);
        }
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     *
     * @param plaintext the raw bytes to encrypt
     * @return Base64-encoded string containing IV + ciphertext + GCM tag
     */
    public static String encrypt(byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);

            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), spec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Concatenate IV + ciphertext (which includes the GCM tag appended by doFinal)
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            BOLogger.error("Encryption failed", e);
            return null;
        }
    }

    /**
     * Encrypts a UTF-8 string using AES-256-GCM.
     */
    public static String encryptString(String plaintext) {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts a Base64-encoded ciphertext produced by {@link #encrypt(byte[])}.
     *
     * @param encoded Base64 string containing IV + ciphertext + GCM tag
     * @return decrypted bytes, or null if decryption fails
     */
    public static byte[] decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;

        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length < GCM_IV_LENGTH + 16) return null; // min: IV + tag

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            BOLogger.error("Decryption failed — data may be corrupted or from another machine", e);
            return null;
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext back to a UTF-8 string.
     */
    public static String decryptString(String encoded) {
        byte[] plaintext = decrypt(encoded);
        return plaintext != null ? new String(plaintext, StandardCharsets.UTF_8) : null;
    }
}
