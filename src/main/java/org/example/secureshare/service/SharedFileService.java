package org.example.secureshare.service;

import jakarta.persistence.criteria.*;
import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.sharedfileDTO.FetchUsersResponse;
import org.example.secureshare.payload.sharedfileDTO.SharedFileResponse;
import org.example.secureshare.payload.sharedfileDTO.SharedFilesResponse;
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
    private AuthUtil authUtil;


    @Autowired
    private FileRepository fileRepository;

    @Transactional
    public void logFileShare(Long oldFileId, Long newFileId, Long recipientId, String isSensitive) {
        User owner = authUtil.getLoggedInUser();
        File newFile = fileRepository.findById(newFileId)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + newFileId));

        SharedFile log = new SharedFile();
        log.setNewFileId(newFileId);
        log.setOriginalFileId(oldFileId);
        log.setSenderId(owner.getUserId());
        log.setRecipientId(recipientId);
        log.setFilename(newFile.getFilename());
        log.setCategory(newFile.getCategory());
        log.setIsSensitive(isSensitive);
        log.setSharedAt(LocalDateTime.now());

        sharedFileRepository.save(log);
    }

    @Transactional(readOnly = true)
    public SharedFilesResponse getFilesSharedByMe(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String sensitive) {
        User owner = authUtil.getLoggedInUser();
        Specification<SharedFile> spec = (root, query, cb) -> cb.equal(root.get("senderId"), owner.getUserId());
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

        if (sensitive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isSensitive"), sensitive));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            spec = spec.and((root, query, cb) -> {
                // Subquery for matching recipient usernames
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<User> userRoot = subquery.from(User.class);

                subquery.select(userRoot.get("userId"))
                        .where(cb.like(cb.lower(userRoot.get("username")), likeKeyword));

                // Match against SharedFile.recipientId
                Predicate recipientMatch = root.get("recipientId").in(subquery);

                // Match against filename/category
                Predicate filenameMatch = cb.like(cb.lower(root.get("filename")), likeKeyword);
                Predicate categoryMatch = cb.like(cb.lower(root.get("category")), likeKeyword);

                return cb.or(recipientMatch, filenameMatch, categoryMatch);
            });
        }


        Page<SharedFile> logs = sharedFileRepository.findAll(spec, pageable);


        SharedFilesResponse response = new SharedFilesResponse();
        List<SharedFileResponse> sharedFileResponse = logs.stream()
                .map(log ->
                {
                   User sender = userRepository.findById(log.getSenderId())
                           .orElseThrow(() -> new NoSuchElementException("Sender not found with ID: " + log.getSenderId()));

                   User recipient = userRepository.findById(log.getRecipientId())
                           .orElseThrow(() -> new NoSuchElementException("Recipient not found with ID: " + log.getRecipientId()));

                    SharedFileResponse sharedFileResponse1 =  new SharedFileResponse(
                            sender.getUsername(),
                            recipient.getUsername(),
                            log.getFilename(),
                            log.getCategory(),
                            Boolean.valueOf(log.getIsSensitive()),
                            log.getSharedAt()

                    );
                     return sharedFileResponse1;
                })
                .toList();
        response.setFetchFiles(sharedFileResponse);
        response.setPageNumber(logs.getNumber() + 1); // Pages are 0
        response.setPageSize(logs.getSize());
        response.setTotalElements(logs.getTotalElements());
        response.setTotalPages(logs.getTotalPages());
        response.setLastPage(logs.isLast());
        return response;
    }

    @Transactional(readOnly = true)
    public SharedFilesResponse getFilesSharedToMe(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String sensitive) {
        User owner = authUtil.getLoggedInUser();
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

        Specification<SharedFile> spec = (root, query, cb) -> cb.equal(root.get("recipientId"), owner.getUserId());

        if (sensitive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isSensitive"), sensitive));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            spec = spec.and((root, query, cb) -> {
                // Subquery for matching recipient usernames
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<User> userRoot = subquery.from(User.class);

                subquery.select(userRoot.get("userId"))
                        .where(cb.like(cb.lower(userRoot.get("username")), likeKeyword));

                // Match against SharedFile.recipientId
                Predicate recipientMatch = root.get("senderId").in(subquery);

                // Match against filename/category
                Predicate filenameMatch = cb.like(cb.lower(root.get("filename")), likeKeyword);
                Predicate categoryMatch = cb.like(cb.lower(root.get("category")), likeKeyword);

                return cb.or(recipientMatch, filenameMatch, categoryMatch);
            });
        }


        Page<SharedFile> logs = sharedFileRepository.findAll(spec, pageable);

        SharedFilesResponse response = new SharedFilesResponse();
        List<SharedFileResponse> sharedFileResponse = logs.stream()
                .map(log ->
                {
                    User sender = userRepository.findById(log.getSenderId())
                            .orElseThrow(() -> new NoSuchElementException("Sender not found with ID: " + log.getSenderId()));

                    User recipient = userRepository.findById(log.getRecipientId())
                            .orElseThrow(() -> new NoSuchElementException("Recipient not found with ID: " + log.getRecipientId()));

                    SharedFileResponse sharedFileResponse1 =  new SharedFileResponse(
                            sender.getUsername(),
                            recipient.getUsername(),
                            log.getFilename(),
                            log.getCategory(),
                            Boolean.valueOf(log.getIsSensitive()),
                            log.getSharedAt()

                    );
                    return sharedFileResponse1;
                })
                .toList();
        response.setFetchFiles(sharedFileResponse);
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

    public FetchUsersResponse getUsersFileIsSharedWith(
            Long fileId, String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        User owner = authUtil.getLoggedInUser();
        File originalFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

        if (originalFile.getOriginalFileId() != null && !originalFile.getOriginalFileId().equals(originalFile.getId())) {
            throw new IllegalArgumentException("The specified file is not an original file owned by the user.");
        }

        if (!originalFile.getOwnerId().equals(owner.getUserId())) {
            throw new SecurityException("User is not authorized to access this file's sharing information.");
        }

        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);

        Specification<SharedFile> spec = (root, query, cb) -> cb.equal(root.get("originalFileId"), originalFile.getId());

        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";

            spec = spec.and((root, query, cb) -> {
                // Subquery for matching recipient usernames
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<User> userRoot = subquery.from(User.class);

                subquery.select(userRoot.get("userId"))
                        .where(cb.like(cb.lower(userRoot.get("username")), likeKeyword));

                // Match against SharedFile.recipientId
                Predicate recipientMatch = root.get("recipientId").in(subquery);
                return cb.or(recipientMatch);
            });
        }

        Page<SharedFile> sharedFiles = sharedFileRepository.findAll(spec, pageable);

        // Convert into DTO with recipient + sender usernames
        FetchUsersResponse response = new FetchUsersResponse();
        List<String> recipientUsernames = sharedFiles.stream()
                .map(sf -> userRepository.findById(sf.getRecipientId())
                        .map(User::getUsername)
                        .orElse("Unknown"))
                .toList();

        response.setUsernames(recipientUsernames);
        response.setPageNumber(sharedFiles.getNumber() + 1); // Pages are 0
        response.setPageSize(sharedFiles.getSize());
        response.setTotalElements(sharedFiles.getTotalElements());
        response.setTotalPages(sharedFiles.getTotalPages());
        response.setLastPage(sharedFiles.isLast());
        return response;
    }
}