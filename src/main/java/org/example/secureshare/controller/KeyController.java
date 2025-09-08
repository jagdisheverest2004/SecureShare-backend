package org.example.secureshare.controller;

import org.example.secureshare.service.KeyDownloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/keys")
public class KeyController {

    @Autowired
    private KeyDownloadService keyDownloadService;

    @GetMapping("/download-public-key")
    public ResponseEntity<?> downloadPublicKey() {
        try {

            Resource resource = keyDownloadService.downloadPublicKey();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"public_key.pem\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/download-private-key")
    public ResponseEntity<?> downloadPrivateKey() {
        try {
            Resource resource = keyDownloadService.downloadPrivateKey();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"private_key.key    \"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error decrypting private key."));
        }
    }


}
