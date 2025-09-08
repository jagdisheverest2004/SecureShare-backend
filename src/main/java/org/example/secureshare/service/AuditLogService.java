package org.example.secureshare.service;

import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.auditDTO.AuditLogResponse;
import org.example.secureshare.payload.auditDTO.AuditLogsResponse;
import org.example.secureshare.repository.AuditLogRepository;
import org.example.secureshare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuthUtil authUtil;


    @Transactional
    public void logAction(Long userId,String action, String filename) {
        AuditLog log = new AuditLog(userId, action, filename);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logAction(String action, String filename) {
        User user = authUtil.getLoggedInUser();
        AuditLog log = new AuditLog(user.getUserId(), action, filename);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public AuditLogsResponse getLogsForUser(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User user = authUtil.getLoggedInUser();
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<AuditLog> logs = auditLogRepository.findByUserId(user.getUserId(), pageable);

        List<AuditLogResponse> auditLogResponses = logs.getContent().stream().map(log -> {
            AuditLogResponse response = new AuditLogResponse();
            response.setUsername(user.getUsername());
            response.setAction(log.getAction());
            response.setFilename(log.getFilename());
            response.setTimestamp(log.getTimestamp());
            return response;
        }).toList();
        AuditLogsResponse response = new AuditLogsResponse();

        response.setAuditLogList(auditLogResponses);
        response.setPageNumber(logs.getNumber() + 1); // Convert to 1-based index
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

    @Transactional
    public void deleteAllLogsForUser() {
        User user = authUtil.getLoggedInUser();
        auditLogRepository.deleteByUserId(user.getUserId());
    }
}