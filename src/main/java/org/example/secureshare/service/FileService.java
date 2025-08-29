package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.FetchFileResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyService keyService;

    @Transactional
    public Long storeFile(MultipartFile file, String fileName, String description, String category, String username) throws IOException {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty.");
            }
            User owner = userRepository.findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

            SecretKey aesKey = keyService.generateAesKey();
            byte[] iv = keyService.generateIV();

            byte[] encryptedFileData = keyService.encryptWithAesGcm(file.getBytes(), aesKey, iv);

            byte[] encryptedAesKeyBytes = keyService.encryptWithRsa(
                    keyService.getAesKeyBytes(aesKey),
                    keyService.decodePublicKey(owner.getPublicKey())
            );
            String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);

            File newFile = new File(encryptedFileData, encryptedAesKeyBase64, Base64.getEncoder().encodeToString(iv), fileName, description, category, owner);
            File savedFile = fileRepository.save(newFile);
            return savedFile.getId();
        } catch (IOException | NoSuchElementException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file due to a cryptographic error.", e);
        }
    }

    @Transactional(readOnly = true)
    public FetchFileResponse getFileByFilename(String filename, String username) {
        // ... (existing code for getFileByFilename)
        try {
            File file = fileRepository.findByFilename(filename)
                    .orElseThrow(() -> new NoSuchElementException("File not found with filename: " + filename));
            User loggedInUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

            if (!file.getOwner().getUserId().equals(loggedInUser.getUserId())) {
                throw new SecurityException("User is not authorized to access this file.");
            }

            return new FetchFileResponse(file.getId(), file.getFilename(), file.getDescription(), file.getCategory());

        } catch (NoSuchElementException | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt file due to a cryptographic error.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FetchFileResponse> getAllFilesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        List<File> files = fileRepository.findByOwnerUserId(user.getUserId());

        // Convert File entities to FetchFileResponse DTOs
        return files.stream()
                .map(file -> new FetchFileResponse(
                        file.getId(),
                        file.getFilename(),
                        file.getDescription(),
                        file.getCategory()
                ))
                .collect(Collectors.toList());
    }
}