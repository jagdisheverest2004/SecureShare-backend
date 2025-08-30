package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFileResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    public List<FetchFileResponse> getAllFilesForUser(String keyword, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // Start with the base specification that filters by the owner's ID.
        Specification<File> spec = (root, query, cb) -> cb.equal(root.get("owner").get("userId"), user.getUserId());

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            // Create a sub-specification for the keyword search using OR conditions.
            Specification<File> keywordSpec = (root, query, cb) -> cb.like(cb.lower(root.get("category")), likeKeyword);
            keywordSpec = keywordSpec.or((root, query, cb) -> cb.like(cb.lower(root.get("description")), likeKeyword));
            keywordSpec = keywordSpec.or((root, query, cb) -> cb.like(cb.lower(root.get("filename")), likeKeyword));

            // Combine the base specification with the keyword search specification.
            spec = spec.and(keywordSpec);
        }

        // Pass the final, combined specification to the repository.
        List<File> files = fileRepository.findAll(spec);

        return files.stream()
                .map(file -> new FetchFileResponse(
                        file.getId(),
                        file.getFilename(),
                        file.getDescription(),
                        file.getCategory()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Long shareFile(Long fileId, String senderUsername, String recipientUsername, boolean isSensitive) {
        try {
            // Fetch sender and recipient users
            User sender = userRepository.findByUsername(senderUsername)
                    .orElseThrow(() -> new NoSuchElementException("Sender not found: " + senderUsername));

            User recipient = userRepository.findByUsername(recipientUsername)
                    .orElseThrow(() -> new NoSuchElementException("Recipient not found: " + recipientUsername));

            // Fetch the file to be shared
            File originalFile = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

            // Authorization check: ensure sender owns the file
            if (!originalFile.getOwner().getUserId().equals(sender.getUserId())) {
                throw new SecurityException("User is not authorized to share this file.");
            }

            // 1. Decrypt AES key with sender's private key
            PrivateKey senderPrivateKey = keyService.decodePrivateKey(sender.getPrivateKey());
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(originalFile.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, senderPrivateKey);
            SecretKey decryptedAesKey = keyService.getAesKeyFromBytes(decryptedAesKeyBytes);

            // 2. Encrypt the same AES key with the recipient's public key
            PublicKey recipientPublicKey = keyService.decodePublicKey(recipient.getPublicKey());
            byte[] encryptedAesKeyForRecipientBytes = keyService.encryptWithRsa(keyService.getAesKeyBytes(decryptedAesKey), recipientPublicKey);
            String encryptedAesKeyForRecipientBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyForRecipientBytes);

            // 3. Create a new File entry for the recipient
            File sharedFile = new File(
                    originalFile.getEncryptedData(),
                    encryptedAesKeyForRecipientBase64,
                    originalFile.getIv(),
                    originalFile.getFilename(),
                    originalFile.getDescription(),
                    originalFile.getCategory(),
                    recipient
            );
            File savedFile = fileRepository.save(sharedFile);

            return savedFile.getId();

        } catch (NoSuchElementException | SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to share file due to a cryptographic error.", e);
        }
    }
}