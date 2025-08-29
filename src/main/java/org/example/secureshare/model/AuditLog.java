package org.example.secureshare.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String action; // e.g., "FILE_UPLOAD", "FILE_SHARED"
    private String filename; // Optional, for file-related actions
    private LocalDateTime timestamp;

    public AuditLog(String username, String action, String filename) {
        this.username = username;
        this.action = action;
        this.filename = filename;
        this.timestamp = LocalDateTime.now();
    }
}