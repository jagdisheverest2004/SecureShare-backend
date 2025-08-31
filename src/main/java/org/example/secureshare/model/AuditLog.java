package org.example.secureshare.model;

import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Link to the User entity
    private User user;

    private String username;
    private String action; // e.g., "FILE_UPLOAD", "FILE_SHARED"
    private String filename; // Optional, for file-related actions
    private LocalDateTime timestamp;

    public AuditLog(User user ,String username, String action, String filename) {
        this.user = user;
        this.username = username;
        this.action = action;
        this.filename = filename;
        this.timestamp = LocalDateTime.now();
    }
}