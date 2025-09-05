package org.example.secureshare.controller;

import org.example.secureshare.config.AppConstants;
import org.example.secureshare.payload.auditDTO.AuditLogsResponse;
import org.example.secureshare.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/my-logs")
    public ResponseEntity<AuditLogsResponse> getMyLogs(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.AUDIT_LOGS_PAGE_SIZE,required = false)  Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstants.SORT_AUDIT_LOGS_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstants.SORT_AUDIT_LOGS_DIR,required = false) String sortOrder
    ) {
        AuditLogsResponse logs = auditLogService.getLogsForUser(pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(logs);
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllLogsForUser() {
        auditLogService.deleteAllLogsForUser();
        return ResponseEntity.ok().body("All audit logs deleted successfully for the user.");
    }
}