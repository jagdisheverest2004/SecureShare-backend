package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFileResponse;
import org.example.secureshare.payload.fiteDTO.FetchFilesResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.SharedFileRepository;
import org.example.secureshare.repository.UserRepository;
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
import java.util.stream.Collectors;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyService keyService;

    @Transactional
    public List<Long> storeFiles(MultipartFile[] files, String description, String category, String username) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files selected for upload.");
        }

        List<Long> uploadedFileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            Long fileId = this.storeSingleFile(file, description, category, username);
            uploadedFileIds.add(fileId);
        }
        return uploadedFileIds;
    }

    @Transactional
    public Long storeSingleFile(MultipartFile file, String description, String category, String username) throws IOException {
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

            File newFile = new File(encryptedFileData, encryptedAesKeyBase64, Base64.getEncoder().encodeToString(iv), file.getOriginalFilename(), description, category, file.getContentType(), owner); // Pass contentType
            File savedFile = fileRepository.save(newFile);

            savedFile.setOriginalFile(savedFile);
            fileRepository.save(savedFile);

            return savedFile.getId();
        } catch (IOException | NoSuchElementException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file due to a cryptographic error.", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> downloadFileAndGetMetadata(Long fileId, String username) {
        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));
            User loggedInUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

            if (!file.getOwner().getUserId().equals(loggedInUser.getUserId())) {
                throw new SecurityException("User is not authorized to access this file.");
            }

            PrivateKey ownerPrivateKey = keyService.decodePrivateKey(loggedInUser.getPrivateKey());
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
    public FetchFilesResponse getAllFilesForUser(String keyword, String username, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

        Specification<File> spec = (root, query, cb) -> cb.equal(root.get("owner").get("userId"), user.getUserId());

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
    public Long shareFile(Long fileId, String senderUsername, String recipientUsername) {
        try {
            User sender = userRepository.findByUsername(senderUsername)
                    .orElseThrow(() -> new NoSuchElementException("Sender not found: " + senderUsername));
            User recipient = userRepository.findByUsername(recipientUsername)
                    .orElseThrow(() -> new NoSuchElementException("Recipient not found: " + recipientUsername));
            File originalFile = fileRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

            if (!originalFile.getOwner().getUserId().equals(sender.getUserId())) {
                throw new SecurityException("User is not authorized to share this file.");
            }

            PrivateKey senderPrivateKey = keyService.decodePrivateKey(sender.getPrivateKey());
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
                    originalFile.getContentType(), // Pass contentType
                    recipient,
                    originalFile // Set the original file reference here
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
    public void deleteFile(Long fileId, String username, String deletionType, List<String> recipientUsernames) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        File originalFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

        if (!originalFile.getOwner().getUserId().equals(owner.getUserId())) {
            throw new SecurityException("User is not authorized to delete this file.");
        }

        switch (deletionType) {
            case "me":
                // Delete only the original file from the sender's account.
                fileRepository.delete(originalFile);
                break;

            case "everyone":
                // 1. Find all shared copies (recipient's files) and their logs
                List<SharedFile> allSharedFileLogs = sharedFileRepository.findByOriginalFile_Id(originalFile.getId());

                // 2. Collect the file IDs of the recipient's copies
                List<Long> recipientFileIds = allSharedFileLogs.stream()
                        .map(log -> log.getFile().getId())
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

                // 1. Find the shared file logs for the specified recipients
                List<SharedFile> sharedFileLogsForRecipients = sharedFileRepository.findByOriginalFile_IdAndRecipient_UsernameIn(originalFile.getId(), recipientUsernames);

                // 2. Collect the file IDs of the recipient's copies
                List<Long> recipientFilesToDeleteIds = sharedFileLogsForRecipients.stream()
                        .map(log -> log.getFile().getId())
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
}