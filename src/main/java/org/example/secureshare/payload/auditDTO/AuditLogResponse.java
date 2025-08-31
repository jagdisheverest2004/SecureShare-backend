package org.example.secureshare.payload.auditDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private String username;
    private String action; // e.g., "FILE_UPLOAD", "FILE_SHARED"
    private String filename; // Optional, for file-related actions
    private LocalDateTime timestamp;
}
