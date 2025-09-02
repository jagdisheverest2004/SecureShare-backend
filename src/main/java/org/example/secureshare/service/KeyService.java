package org.example.secureshare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class KeyService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_SIZE = 256;

    @Value("${MASTER_KEY}")
    private String masterKeyBase64;

    private SecretKey getMasterKey() {
        byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    // New methods for master key encryption/decryption
    public String encryptPrivateKey(PrivateKey privateKey) throws Exception {
        byte[] privateKeyBytes = privateKey.getEncoded();
        SecretKey masterKey = getMasterKey();
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmParameterSpec);
        byte[] encryptedKeyBytes = cipher.doFinal(privateKeyBytes);
        byte[] combined = new byte[iv.length + encryptedKeyBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedKeyBytes, 0, combined, iv.length, encryptedKeyBytes.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    public PrivateKey decryptPrivateKey(String encryptedPrivateKeyBase64) throws Exception {
        byte[] encryptedCombined = Base64.getDecoder().decode(encryptedPrivateKeyBase64);
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedCombined, 0, iv, 0, iv.length);
        byte[] encryptedKeyBytes = new byte[encryptedCombined.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedCombined, GCM_IV_LENGTH, encryptedKeyBytes, 0, encryptedKeyBytes.length);
        SecretKey masterKey = getMasterKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmParameterSpec);
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decryptedKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    // existing methods...
    public KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public String encodePublicKey(PublicKey publicKey) { return Base64.getEncoder().encodeToString(publicKey.getEncoded()); }
    public String encodePrivateKey(PrivateKey privateKey) { return Base64.getEncoder().encodeToString(privateKey.getEncoded()); }
    public PublicKey decodePublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
    public byte[] encryptWithRsa(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public byte[] decryptWithRsa(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    public SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }

    public byte[] getAesKeyBytes(SecretKey secretKey) {
        return secretKey.getEncoded();
    }

    public SecretKey getAesKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encryptWithAesGcm(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
        return cipher.doFinal(data);
    }

    public byte[] decryptWithAesGcm(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
        return cipher.doFinal(encryptedData);
    }

    public byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}