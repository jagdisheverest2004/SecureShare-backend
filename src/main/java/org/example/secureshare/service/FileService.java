package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFileResponse;
import org.example.secureshare.payload.fiteDTO.FetchFilesResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.SharedFileRepository;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.util.AuthUtil;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class FileService {

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

            SecretKey aesKey = keyService.generateAesKey();
            byte[] iv = keyService.generateIV();

            byte[] encryptedFileData = keyService.encryptWithAesGcm(file.getBytes(), aesKey, iv);

            byte[] encryptedAesKeyBytes = keyService.encryptWithRsa(
                    keyService.getAesKeyBytes(aesKey),
                    keyService.decodePublicKey(owner.getPublicKey())
            );
            String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);

            File newFile = new File(encryptedFileData, encryptedAesKeyBase64, Base64.getEncoder().encodeToString(iv), file.getOriginalFilename(), description, category, file.getContentType(), owner.getUserId());
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

    // Add this new method to handle both data and metadata retrieval
    @Transactional(readOnly = true)
    public Map<String, Object> downloadFileAndGetMetadata(Long fileId) {
        try {
            User owner = authUtil.getLoggedInUser();
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

            if (!file.getOwnerId().equals(owner.getUserId())) {
                throw new SecurityException("User is not authorized to access this file.");
            }

            // Decrypt the user's private key with the master key before using it.
            PrivateKey ownerPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(file.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, ownerPrivateKey);
            SecretKey decryptedAesKey = keyService.getAesKeyFromBytes(decryptedAesKeyBytes);
            byte[] iv = Base64.getDecoder().decode(file.getIv());
            byte[] decryptedFileData = keyService.decryptWithAesGcm(file.getEncryptedData(), decryptedAesKey, iv);

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
    public Long shareFile(Long fileId, User recipient) {
        try {
            User owner = authUtil.getLoggedInUser();
            File originalFile = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

            if (!originalFile.getOwnerId().equals(owner.getUserId())) {
                throw new SecurityException("User is not authorized to share this file.");
            }

            // Decrypt the sender's private key with the master key before using it.
            PrivateKey senderPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(originalFile.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, senderPrivateKey);
            SecretKey decryptedAesKey = keyService.getAesKeyFromBytes(decryptedAesKeyBytes);

            PublicKey recipientPublicKey = keyService.decodePublicKey(recipient.getPublicKey());
            byte[] encryptedAesKeyForRecipientBytes = keyService.encryptWithRsa(keyService.getAesKeyBytes(decryptedAesKey), recipientPublicKey);
            String encryptedAesKeyForRecipientBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyForRecipientBytes);

            File sharedFile = new File(
                    originalFile.getEncryptedData(),
                    encryptedAesKeyForRecipientBase64,
                    originalFile.getIv(),
                    originalFile.getFilename(),
                    originalFile.getDescription(),
                    originalFile.getCategory(),
                    originalFile.getContentType(),
                    recipient.getUserId(),
                    originalFile.getOriginalFileId()
            );
            File savedFile = fileRepository.save(sharedFile);

            return savedFile.getId();

        } catch (NoSuchElementException | SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to share file due to a cryptographic error.", e);
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
                    // Delete only the original file from the sender's account.
                    fileRepository.delete(originalFile);
                    break;

                case "everyone":
                    // 1. Find all shared copies (recipient's files) and their logs
                    List<SharedFile> allSharedFileLogs = sharedFileRepository.findSharedFilesByFileId(fileId);
                    // 2. Collect the file IDs of the recipient's copies
                    List<Long> recipientFileIds = allSharedFileLogs.stream()
                            .map(SharedFile::getNewFileId)
                            .toList();
                    // 3. Delete the shared file logs FIRST
                    sharedFileRepository.deleteAll(allSharedFileLogs);
                    // 4. Then, delete all the recipient's file copies
                    fileRepository.deleteAllById(recipientFileIds);
                    // 5. Finally, delete the original file from the sender's wallet
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

                    // 1. Find the shared file logs for the specified recipients
                    List<SharedFile> sharedFileLogsForRecipients = sharedFileRepository.findSharedFilesByFileIdAndRecipientId(fileId, recipientIds);
                    if (sharedFileLogsForRecipients.isEmpty()) {
                        throw new NoSuchElementException("No shared file logs found for the specified recipients.");
                    }

                    // 2. Collect the file IDs of the recipient's copies
                    List<Long> recipientFilesToDeleteIds = sharedFileLogsForRecipients.stream()
                            .map(SharedFile::getNewFileId)
                            .toList();

                    // 3. Delete the shared file logs FIRST
                    sharedFileRepository.deleteAll(sharedFileLogsForRecipients);

                    // 4. Then, delete the file records from the specified recipients' wallets
                    fileRepository.deleteAllById(recipientFilesToDeleteIds);

                    // 5. Delete the original file from the sender's wallet
                    fileRepository.delete(originalFile);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid deletion type: " + deletionType);
            }
        }
        else{
            if (!"me".equals(deletionType)) {
                // A recipient should only be able to delete their own copy
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

            // Step 1: Decrypt owner’s private key
            PrivateKey ownerPrivateKey = keyService.decryptPrivateKey(owner.getPrivateKey());

            // Step 2: Decrypt AES key (Base64 → bytes → RSA decrypt)
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(file.getEncryptedAesKey());
            byte[] decryptedAesKeyBytes = keyService.decryptWithRsa(encryptedAesKeyBytes, ownerPrivateKey);

            String aesKeyBase64 = Base64.getEncoder().encodeToString(decryptedAesKeyBytes);
            String ivBase64 = file.getIv(); // already Base64 in DB

            // Step 3: Send email with key + iv
            emailService.sendKeyAndIvJson(owner.getEmail(), aesKeyBase64, ivBase64);

            // Step 4: Return encrypted file bytes + metadata
            Map<String, Object> result = new HashMap<>();
            result.put("encryptedFileData", file.getEncryptedData());
            result.put("originalFilename", file.getFilename() + ".enc"); // ensure .enc extension
            result.put("contentType", file.getContentType());
            return result;

        } catch (NoSuchElementException | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare encrypted download.", e);
        }
    }


}
