package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.sharedfileDTO.SharedFileResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.SharedFileRepository;
import org.example.secureshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class SharedFileService {

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Transactional
    public void logFileShare(Long fileId, Long senderId, Long recipientId, boolean isSensitive) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + fileId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("Sender not found: " + senderId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new NoSuchElementException("Recipient not found: " + recipientId));

        SharedFile log = new SharedFile();
        log.setFile(file);
        log.setSender(sender);
        log.setRecipient(recipient);
        log.setFilename(file.getFilename());
        log.setCategory(file.getCategory());
        log.setSensitive(isSensitive);
        log.setSharedAt(LocalDateTime.now());

        sharedFileRepository.save(log);

    }

    @Transactional(readOnly = true)
    public List<SharedFileResponse> getFilesSharedByMe(String keyword, Boolean sensitive, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // The base specification is a crucial filter for the logged-in user's ID.
        Specification<SharedFile> spec = (root, query, cb) -> cb.equal(root.get("sender").get("userId"), user.getUserId());

        if (sensitive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isSensitive"), sensitive));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            // Use a sub-specification with `OR` conditions for the keyword search.
            Specification<SharedFile> keywordSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("filename")), likeKeyword),
                    cb.like(cb.lower(root.get("category")), likeKeyword),
                    cb.like(cb.lower(root.get("recipient").get("username")), likeKeyword)
            );
            spec = spec.and(keywordSpec);
        }

        List<SharedFile> logs = sharedFileRepository.findAll(spec);

        return logs.stream()
                .map(log -> new SharedFileResponse(
                        log.getSender().getUsername(),
                        log.getRecipient().getUsername(),
                        log.getFilename(),
                        log.getCategory(),
                        log.isSensitive(),
                        log.getSharedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SharedFileResponse> getFilesSharedToMe(String keyword, Boolean sensitive, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // The base specification filters for the authenticated user as the recipient.
        Specification<SharedFile> spec = (root, query, cb) -> cb.equal(root.get("recipient").get("userId"), user.getUserId());

        if (sensitive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isSensitive"), sensitive));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            // Use a sub-specification with `OR` conditions for the keyword search.
            Specification<SharedFile> keywordSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("filename")), likeKeyword),
                    cb.like(cb.lower(root.get("category")), likeKeyword),
                    cb.like(cb.lower(root.get("sender").get("username")), likeKeyword)
            );
            spec = spec.and(keywordSpec);
        }

        List<SharedFile> logs = sharedFileRepository.findAll(spec);

        return logs.stream()
                .map(log -> new SharedFileResponse(
                        log.getSender().getUsername(),
                        log.getRecipient().getUsername(),
                        log.getFilename(),
                        log.getCategory(),
                        log.isSensitive(),
                        log.getSharedAt()
                ))
                .collect(Collectors.toList());
    }
}