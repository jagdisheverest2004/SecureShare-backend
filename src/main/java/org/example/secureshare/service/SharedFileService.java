package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.sharedfileDTO.SharedFileResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.SharedFileRepository;
import org.example.secureshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<SharedFileResponse> getFilesSharedByMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        List<SharedFile> logs = sharedFileRepository.findBySenderUserId(user.getUserId());
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
    public List<SharedFileResponse> getFilesSharedToMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        List<SharedFile> logs = sharedFileRepository.findByRecipientUserId(user.getUserId());
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