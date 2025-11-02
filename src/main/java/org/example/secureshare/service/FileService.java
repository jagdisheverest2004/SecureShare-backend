package org.example.secureshare.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFileResponse;
import org.example.secureshare.payload.fiteDTO.FetchFilesResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.SharedFileRepository;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyService keyService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SharedFileService sharedFileService;

    @Data
    @AllArgsConstructor
    private static class FileLobData {
        byte[] encryptedData;
        String encryptedAesKey;
        String iv;
        String authTag;
        String signature;
        String filename;
        String description;
        String category;
        String contentType;
        Long originalFileId;
        Long ownerId;
        String ownerPublicKey;
    }


    @Transactional(readOnly = true)
    public FileLobData getFileLobDataForSharing(Long fileId, Long ownerId) {
        logger.debug("Reading LOB data for file ID: {}", fileId);
        File originalFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

        if (!originalFile.getOwnerId().equals(ownerId)) {
            throw new SecurityException("User is not authorized to share this file.");
        }

        // Eagerly fetch owner's public key
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + ownerId));
        String ownerPublicKey = owner.getPublicKey();

        // --- Force eager loading of ALL LOBs ---
        // This copies the LOB data out of the stream and into a simple byte array
        byte[] encryptedData = Arrays.copyOf(originalFile.getEncryptedData(), originalFile.getEncryptedData().length);

        // This forces the TEXT LOBs to be read into memory as new Strings
        String encryptedAesKey = new String(originalFile.getEncryptedAesKey());
        String iv = new String(originalFile.getIv());
        String authTag = new String(originalFile.getAuthTag());
        String signature = new String(originalFile.getSignature());

        logger.debug("Successfully read all LOB data for file ID: {}", fileId);

        // Return the DTO with all data safely in memory
        return new FileLobData(
                encryptedData,
                encryptedAesKey,
                iv,
                authTag,
                signature,
                originalFile.getFilename(),
                originalFile.getDescription(),
                originalFile.getCategory(),
                originalFile.getContentType(),
                originalFile.getOriginalFileId(),
                originalFile.getOwnerId(),
                ownerPublicKey
        );
    }


    @Transactional
    public List<Long> storeFiles(MultipartFile[] files, String description, String category) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files selected for upload.");
        }

        List<Long> uploadedFileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            Long fileId = this.storeSingleFile(file, description, category);
            uploadedFileIds.add(fileId);
        }
        return uploadedFileIds;
    }

    @Transactional
    public Long storeSingleFile(MultipartFile file, String description, String category) throws IOException {
        try {
            User owner = authUtil.getLoggedInUser();
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty.");
            }

            String metadata = file.getOriginalFilename() + description + category;
            PrivateKey ownerPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());
            byte[] signatureBytes = keyService.signData(metadata.getBytes(), ownerPrivateKey);
            String signature = Base64.getEncoder().encodeToString(signatureBytes);


            SecretKey aesKey = keyService.generateAesKey();
            byte[] iv = keyService.generateIV();

            // Encrypt the file and get the combined data (ciphertext + tag)
            byte[] encryptedCombinedData = keyService.encryptWithAesGcm(file.getBytes(), aesKey, iv);

            // Separate the ciphertext and the authentication tag
            int tagLength = 16;
            byte[] encryptedData = Arrays.copyOfRange(encryptedCombinedData, 0, encryptedCombinedData.length - tagLength);
            byte[] authTagBytes = Arrays.copyOfRange(encryptedCombinedData, encryptedCombinedData.length - tagLength, encryptedCombinedData.length);

            String authTagBase64 = Base64.getEncoder().encodeToString(authTagBytes);

            byte[] encryptedAesKeyBytes = keyService.encryptWithRsa(
                    keyService.getAesKeyBytes(aesKey),
                    keyService.decodePublicKey(owner.getPublicKey())
            );
            String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);

            // Save the file with the separated ciphertext and tag
            File newFile = new File(); // Use the default constructor
            newFile.setEncryptedData(encryptedData);
            newFile.setSignature(signature);
            newFile.setEncryptedAesKey(encryptedAesKeyBase64);
            newFile.setIv(Base64.getEncoder().encodeToString(iv));
            newFile.setAuthTag(authTagBase64);
            newFile.setFilename(file.getOriginalFilename());
            newFile.setDescription(description);
            newFile.setCategory(category);
            newFile.setContentType(file.getContentType());
            newFile.setOwnerId(owner.getUserId());
            newFile.setOriginalFileId(null);
            newFile.setTimestamp(java.time.LocalDateTime.now());

            File savedFile = fileRepository.save(newFile);

            savedFile.setOriginalFileId(savedFile.getId());
            fileRepository.save(savedFile);

            return savedFile.getId();
        } catch (IOException | NoSuchElementException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file due to a cryptographic error.", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> downloadFileAndGetMetadata(Long fileId) {
        try {
            User owner = authUtil.getLoggedInUser();
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

            if (!file.getOwnerId().equals(owner.getUserId())) {
                throw new SecurityException("User is not authorized to access this file.");
            }

            // Verify the file signature to ensure integrity
            Long originalOwnerId = fileRepository.findByIdAndOriginalFileId(file.getOriginalFileId());
            User originalOwner = userRepository.findById(originalOwnerId)
                    .orElseThrow(() -> new NoSuchElementException("Original file owner not found with ID: " + originalOwnerId));

            String metadata = file.getFilename() + file.getDescription() + file.getCategory();
            boolean isSignatureValid = keyService.verifySignature(metadata.getBytes(), Base64.getDecoder().decode(file.getSignature()), keyService.decodePublicKey(originalOwner.getPublicKey()));
            if (!isSignatureValid) {
                throw new SecurityException("File integrity check failed: Invalid signature.");
            }

            PrivateKey ownerPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(file.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, ownerPrivateKey);
            SecretKey decryptedAesKey = keyService.getAesKeyFromBytes(decryptedAesKeyBytes);
            byte[] iv = Base64.getDecoder().decode(file.getIv());

            // Re-combine the encrypted data and the GCM tag for decryption
            byte[] decryptedFileData = keyService.decryptWithAesGcm(combineCiphertextAndTag(file.getEncryptedData(), file.getAuthTag()), decryptedAesKey, iv);

            Map<String, Object> result = new HashMap<>();
            result.put("fileData", decryptedFileData);
            result.put("originalFilename", file.getFilename());
            result.put("contentType", file.getContentType());
            return result;

        } catch (NoSuchElementException | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file due to a cryptographic error.", e);
        }
    }

    // Utility method to combine ciphertext and tag for decryption
    private byte[] combineCiphertextAndTag(byte[] encryptedData, String authTagBase64) {
        byte[] authTagBytes = Base64.getDecoder().decode(authTagBase64);
        byte[] combined = new byte[encryptedData.length + authTagBytes.length];
        System.arraycopy(encryptedData, 0, combined, 0, encryptedData.length);
        System.arraycopy(authTagBytes, 0, combined, encryptedData.length, authTagBytes.length);
        return combined;
    }

    @Transactional(readOnly = true)
    public FetchFilesResponse getAllFilesForUser(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User owner = authUtil.getLoggedInUser();
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

        Specification<File> spec = (root, query, cb) -> cb.equal(root.get("ownerId"), owner.getUserId());

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            Specification<File> keywordSpec = (root, query, cb) -> cb.like(cb.lower(root.get("category")), likeKeyword);
            keywordSpec = keywordSpec.or((root, query, cb) -> cb.like(cb.lower(root.get("description")), likeKeyword));
            keywordSpec = keywordSpec.or((root, query, cb) -> cb.like(cb.lower(root.get("filename")), likeKeyword));

            spec = spec.and(keywordSpec);
        }

        Page<File> files = fileRepository.findAll(spec, pageable);
        FetchFilesResponse response = new FetchFilesResponse();
        List<FetchFileResponse> fetchFileResponses = files.stream()
                .map(file -> new FetchFileResponse(
                        file.getId(),
                        file.getFilename(),
                        file.getDescription(),
                        file.getCategory(),
                        file.getTimestamp()
                ))
                .toList();
        response.setFetchFiles(fetchFileResponses);
        response.setPageNumber(files.getNumber() + 1);
        response.setPageSize(files.getSize());
        response.setTotalElements(files.getTotalElements());
        response.setTotalPages(files.getTotalPages());
        response.setLastPage(files.isLast());
        return response;
    }

    @Transactional
    public Long shareFile(Long fileId, String recipientUsername, Boolean isSensitive) { // <-- Note the new 'isSensitive' parameter
        try {
            User owner = authUtil.getLoggedInUser();
            logger.debug("Initiating share for file ID: {} from user: {} to user: {}", fileId, owner.getUsername(), recipientUsername);

            // --- STEP 1: READ (in a separate, read-only transaction) ---
            FileLobData originalFileData = this.getFileLobDataForSharing(fileId, owner.getUserId());

            // --- STEP 2: PROCESS (in memory) ---
            User recipient = userRepository.findByUsername(recipientUsername)
                    .orElseThrow(() -> new NoSuchElementException("Recipient not found with username: " + recipientUsername));

            // This check now works reliably
            if (fileRepository.existbyOriginalFileIdAndOwnerId(fileId, recipient.getUserId())) {
                logger.warn("Share failed: Recipient {} already has access to file ID: {}", recipientUsername, fileId);
                throw new IllegalArgumentException("Recipient already has access to this file.");
            }

            String metadata = originalFileData.getFilename() + originalFileData.getDescription() + originalFileData.getCategory();
            boolean isSignatureValid = keyService.verifySignature(
                    metadata.getBytes(),
                    Base64.getDecoder().decode(originalFileData.getSignature()),
                    keyService.decodePublicKey(originalFileData.getOwnerPublicKey())
            );
            if (!isSignatureValid) {
                throw new SecurityException("File integrity check failed: Invalid signature.");
            }

            PrivateKey senderPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(originalFileData.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, senderPrivateKey);
            SecretKey decryptedAesKey = keyService.getAesKeyFromBytes(decryptedAesKeyBytes);

            PublicKey recipientPublicKey = keyService.decodePublicKey(recipient.getPublicKey());
            byte[] encryptedAesKeyForRecipientBytes = keyService.encryptWithRsa(keyService.getAesKeyBytes(decryptedAesKey), recipientPublicKey);
            String encryptedAesKeyForRecipientBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyForRecipientBytes);

            // --- STEP 3: WRITE (in a new transaction) ---
            logger.debug("All checks passed. Saving new file copy for user: {}", recipientUsername);
            File sharedFile = new File();
            sharedFile.setEncryptedData(originalFileData.getEncryptedData()); // Use the eagerly loaded byte array
            sharedFile.setSignature(originalFileData.getSignature());
            sharedFile.setEncryptedAesKey(encryptedAesKeyForRecipientBase64);
            sharedFile.setIv(originalFileData.getIv());
            sharedFile.setAuthTag(originalFileData.getAuthTag());
            sharedFile.setFilename(originalFileData.getFilename());
            sharedFile.setDescription(originalFileData.getDescription());
            sharedFile.setCategory(originalFileData.getCategory());
            sharedFile.setContentType(originalFileData.getContentType());
            sharedFile.setOwnerId(recipient.getUserId());
            sharedFile.setOriginalFileId(originalFileData.getOriginalFileId());
            sharedFile.setTimestamp(java.time.LocalDateTime.now());

            File savedFile = fileRepository.save(sharedFile);
            logger.debug("New file copy saved with ID: {}", savedFile.getId());

            // --- STEP 4: LOG (in the *same* transaction) ---
            // This is the fix for the "Received Files" page.
            // This code now runs *with* the file save.
            sharedFileService.logFileShare(
                    fileId, // original file ID
                    savedFile.getId(), // new file ID
                    recipient.getUserId(),
                    String.valueOf(isSensitive)
            );
            logger.debug("Share log created for new file ID: {}", savedFile.getId());

            return savedFile.getId();

        } catch (NoSuchElementException | SecurityException | IllegalArgumentException e) {
            // Re-throw these to be caught by the controller as 4xx errors
            logger.warn("Share failed ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // All other errors (crypto, LOB, etc.)
            logger.error("Failed to share file due to a critical error.", e);
            // Throw a new RuntimeException to ensure the transaction is rolled back
            throw new RuntimeException("Failed to share file: " + e.getMessage(), e);
        }
    }

    private Pageable getPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(Sort.Direction.ASC, sortBy) : Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(pageNumber -1, pageSize,sortByAndOrder);
        return pageable;
    }

    @Transactional
    public void deleteFile(Long fileId, String deletionType, List<String> recipientUsernames) {
        User owner = authUtil.getLoggedInUser();
        File originalFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

        if (!originalFile.getOwnerId().equals(owner.getUserId())) {
            throw new SecurityException("User is not authorized to delete this file.");
        }

        if(fileRepository.existbyOriginalFileIdAndFileId(fileId)) {
            switch (deletionType) {
                case "me":
                    fileRepository.delete(originalFile);
                    break;
                case "everyone":
                    List<SharedFile> allSharedFileLogs = sharedFileRepository.findSharedFilesByFileId(fileId);
                    List<Long> recipientFileIds = allSharedFileLogs.stream()
                            .map(SharedFile::getNewFileId)
                            .toList();
                    sharedFileRepository.deleteAll(allSharedFileLogs);
                    fileRepository.deleteAllById(recipientFileIds);
                    fileRepository.delete(originalFile);
                    break;
                case "list":
                    if (recipientUsernames == null || recipientUsernames.isEmpty()) {
                        throw new IllegalArgumentException("Recipient usernames cannot be empty for 'list' deletion.");
                    }

                    List<Long> recipientIds = new ArrayList<>();
                    for (String username : recipientUsernames) {
                        User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new NoSuchElementException("Recipient user not found: " + username));
                        recipientIds.add(user.getUserId());
                    }

                    List<SharedFile> sharedFileLogsForRecipients = sharedFileRepository.findSharedFilesByFileIdAndRecipientId(fileId, recipientIds);
                    if (sharedFileLogsForRecipients.isEmpty()) {
                        throw new NoSuchElementException("No shared file logs found for the specified recipients.");
                    }

                    List<Long> recipientFilesToDeleteIds = sharedFileLogsForRecipients.stream()
                            .map(SharedFile::getNewFileId)
                            .toList();

                    sharedFileRepository.deleteAll(sharedFileLogsForRecipients);
                    fileRepository.deleteAllById(recipientFilesToDeleteIds);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid deletion type: " + deletionType);
            }
        } else {
            if (!"me".equals(deletionType)) {
                throw new IllegalArgumentException("Deletion type must be 'me' for a shared file copy.");
            }
            fileRepository.delete(originalFile);
            sharedFileRepository.deleteByNewFileId(fileId);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> downloadEncryptedFileAndSendKeys(Long fileId) {
        try {
            User owner = authUtil.getLoggedInUser();
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

            if (!file.getOwnerId().equals(owner.getUserId())) {
                throw new SecurityException("User is not authorized to access this file.");
            }

            PrivateKey ownerPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(file.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, ownerPrivateKey);

            String aesKeyBase64 = Base64.getEncoder().encodeToString(decryptedAesKeyBytes);
            String ivBase64 = file.getIv();
            String authTagBase64 = file.getAuthTag();

            emailService.sendKeyIvAndTagJson(owner.getEmail(), aesKeyBase64, ivBase64, authTagBase64);

            Map<String, Object> result = new HashMap<>();
            result.put("encryptedFileData", file.getEncryptedData());
            result.put("originalFilename", file.getFilename() + ".enc");
            result.put("contentType", file.getContentType());
            return result;

        } catch (NoSuchElementException | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare encrypted download.", e);
        }
    }
}