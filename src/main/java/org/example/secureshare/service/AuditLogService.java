package org.example.secureshare.service;

import org.example.secureshare.model.AuditLog;
import org.example.secureshare.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(String username, String action, String filename) {
        AuditLog log = new AuditLog(username, action, filename);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsForUser(String username) {
        return auditLogRepository.findByUsername(username);
    }
}