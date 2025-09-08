package org.example.secureshare.service;

import org.example.secureshare.model.User;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;

@Service
public class KeyDownloadService {

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private KeyService keyService;

    @Autowired
    private UserRepository userRepository;

    public Resource downloadPublicKey() {
        User user = authUtil.getLoggedInUser();

        if (user.getPublicKey() == null) {
            throw new IllegalStateException("No public key available for user.");
        }

        String encodedKey = user.getPublicKey();
        String pemFormatted =
                "-----BEGIN PUBLIC KEY-----\n" +
                        chunkString(encodedKey) +
                        "-----END PUBLIC KEY-----\n";

        byte[] keyBytes = pemFormatted.getBytes();
        Resource resource = new ByteArrayResource(keyBytes);

        return resource;
    }

    public Resource downloadPrivateKey() throws Exception {
        User user = authUtil.getLoggedInUser();

        if (user.getPrivateKey() == null) {
            throw new IllegalStateException("No private key available for user.");
        }

        PrivateKey privateKey = keyService.decryptPrivateKey(user.getPrivateKey());
        String encodedKey = keyService.encodePrivateKey(privateKey);
        String pemFormatted =
                "-----BEGIN PUBLIC KEY-----\n" +
                        chunkString(encodedKey) +
                        "-----END PUBLIC KEY-----\n";

        byte[] keyBytes = pemFormatted.getBytes();
        Resource resource = new ByteArrayResource(keyBytes);

        return resource;
    }

    // ✅ Helper: split long Base64 string into 64-char lines (PEM standard)
    private String chunkString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i += 64) {
            sb.append(str, i, Math.min(str.length(), i + 64)).append("\n");
        }
        return sb.toString();
    }
}
