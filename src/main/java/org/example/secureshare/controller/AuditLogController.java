package org.example.secureshare.controller;

import org.example.secureshare.config.AppConstants;
import org.example.secureshare.model.AuditLog;
import org.example.secureshare.payload.auditDTO.AuditLogsReponse;
import org.example.secureshare.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/my-logs")
    public ResponseEntity<AuditLogsReponse> getMyLogs(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.AUDIT_LOGS_PAGE_SIZE,required = false)  Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstants.SORT_AUDIT_LOGS_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstants.SORT_AUDIT_LOGS_DIR,required = false) String sortOrder
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AuditLogsReponse logs = auditLogService.getLogsForUser(username, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(logs);
    }
}