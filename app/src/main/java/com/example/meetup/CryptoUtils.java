package com.example.meetup;

import android.util.Base64;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    public static String encrypt(String input, String key) throws Exception {
        return Base64.encodeToString(doCrypto(Cipher.ENCRYPT_MODE, input, key), Base64.DEFAULT);
    }

    public static String decrypt(String input, String key) throws Exception {
        byte[] decodedValue = Base64.decode(input, Base64.DEFAULT);
        byte[] decryptedValue = doCrypto(Cipher.DECRYPT_MODE, decodedValue, key);
        return new String(decryptedValue);
    }

    private static byte[] doCrypto(int cipherMode, String input, String key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        cipher.init(cipherMode, secretKey);
        return cipher.doFinal(input.getBytes());
    }

    private static byte[] doCrypto(int cipherMode, byte[] input, String key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        cipher.init(cipherMode, secretKey);
        return cipher.doFinal(input);
    }
}
