package org.example.secureshare.service;

import org.springframework.stereotype.Service;
import java.security.*;
import java.util.Base64;

@Service
public class KeyService {

    public KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // Recommended key size for security
        return keyPairGenerator.generateKeyPair();
    }

    public String convertPublicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String convertPrivateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}