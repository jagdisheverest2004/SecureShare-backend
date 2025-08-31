package org.example.secureshare.service;

import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.auditDTO.AuditLogsReponse;
import org.example.secureshare.repository.AuditLogRepository;
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

    @Transactional
    public void logAction(User user , String username, String action, String filename) {
        AuditLog log = new AuditLog(user,username, action, filename);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public AuditLogsReponse getLogsForUser(String username, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageable = getPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<AuditLog> logs = auditLogRepository.findByUsername(username, pageable);

        AuditLogsReponse response = new AuditLogsReponse();
        response.setAuditLogList(logs.getContent());
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
}