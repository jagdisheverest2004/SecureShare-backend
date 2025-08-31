package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.sharedfileDTO.SharedFileResponse;
import org.example.secureshare.payload.sharedfileDTO.SharedFilesResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SharedFileService {

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Transactional
    public void logFileShare(Long oldFileId, Long newFileId, Long senderId, Long recipientId, String isSensitive) {
        File newFile = fileRepository.findById(newFileId)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + newFileId));

        File originalFile = fileRepository.findById(oldFileId)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + oldFileId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("Sender not found: " + senderId));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new NoSuchElementException("Recipient not found: " + recipientId));

        SharedFile log = new SharedFile();
        log.setFile(newFile);
        log.setOriginalFile(originalFile);
        log.setSender(sender);
        log.setRecipient(recipient);
        log.setFilename(newFile.getFilename());
        log.setCategory(newFile.getCategory());
        log.setIsSensitive(isSensitive);
        log.setSharedAt(LocalDateTime.now());

        sharedFileRepository.save(log);
    }

    @Transactional(readOnly = true)
    public SharedFilesResponse getFilesSharedByMe(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String sensitive, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // The base specification is a crucial filter for the logged-in user's ID.
        Specification<SharedFile> spec = (root, query, cb) -> cb.equal(root.get("sender").get("userId"), user.getUserId());
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

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

        Page<SharedFile> logs = sharedFileRepository.findAll(spec, pageable);


        SharedFilesResponse response = new SharedFilesResponse();
        List<SharedFileResponse> sharedFileResponse = logs.stream()
                .map(log -> new SharedFileResponse(
                        log.getSender().getUsername(),
                        log.getRecipient().getUsername(),
                        log.getFilename(),
                        log.getCategory(),
                        log.getIsSensitive(),
                        log.getSharedAt()
                ))
                .toList();
        response.setSharedFiles(sharedFileResponse);
        response.setPageNumber(logs.getNumber() + 1); // Pages are 0
        response.setPageSize(logs.getSize());
        response.setTotalElements(logs.getTotalElements());
        response.setTotalPages(logs.getTotalPages());
        response.setLastPage(logs.isLast());
        return response;
    }

    @Transactional(readOnly = true)
    public SharedFilesResponse getFilesSharedToMe(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String sensitive, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

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

        Page<SharedFile> logs = sharedFileRepository.findAll(spec, pageable);

        SharedFilesResponse response = new SharedFilesResponse();
        List<SharedFileResponse> sharedFileResponse = logs.stream()
                .map(log -> new SharedFileResponse(
                        log.getSender().getUsername(),
                        log.getRecipient().getUsername(),
                        log.getFilename(),
                        log.getCategory(),
                        log.getIsSensitive(),
                        log.getSharedAt()
                ))
                .toList();
        response.setSharedFiles(sharedFileResponse);
        response.setPageNumber(logs.getNumber() + 1); // Pages are 0
        response.setPageSize(logs.getSize());
        response.setTotalElements(logs.getTotalElements());
        response.setTotalPages(logs.getTotalPages());
        response.setLastPage(logs.isLast());
        return response;
    }

    private Pageable getPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(Sort.Direction.ASC, sortBy) : Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(pageNumber -1, pageSize,sortByAndOrder);
        return pageable;
    }
}