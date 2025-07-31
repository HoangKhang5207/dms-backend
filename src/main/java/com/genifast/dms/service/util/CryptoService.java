package com.genifast.dms.service.util;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH_BYTES = 16; // For AES, IV is 16 bytes
    private static final int KEY_LENGTH_BITS = 256;
    private static final int ITERATIONS = 65536;

    // WARNING: Using a fixed salt is a security risk. This is done to meet the requirement
    // of not modifying the database structure. In a real application, a unique salt
    // should be generated and stored for each encrypted document.
    private static final byte[] FIXED_SALT = "ThisIsAFixedSaltForDemo".getBytes(); 

    public byte[] encrypt(byte[] data, String password) throws Exception {
        // Generate IV
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Derive key from password using PBKDF2 with fixed salt
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), FIXED_SALT, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeySpec secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] encryptedBytes = cipher.doFinal(data);

        // Prepend IV to the encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return combined;
    }

    public byte[] decrypt(byte[] combinedData, String password) throws Exception {
        // Extract IV and encrypted data
        byte[] iv = new byte[IV_LENGTH_BYTES];
        System.arraycopy(combinedData, 0, iv, 0, iv.length);
        byte[] encryptedBytes = new byte[combinedData.length - iv.length];
        System.arraycopy(combinedData, iv.length, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Derive key from password using PBKDF2 with fixed salt
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), FIXED_SALT, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeySpec secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        return cipher.doFinal(encryptedBytes);
    }

    public String encryptString(String data, String password) throws Exception {
        byte[] encryptedBytes = encrypt(data.getBytes("UTF-8"), password);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decryptString(String encryptedData, String password) throws Exception {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = decrypt(decodedBytes, password);
        return new String(decryptedBytes, "UTF-8");
    }
}
