package org.example.secureshare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String action;
    private String filename;
    private LocalDateTime timestamp;

    public AuditLog(Long userId, String action, String filename) {
        this.userId = userId;
        this.action = action;
        this.filename = filename;
        this.timestamp = LocalDateTime.now();
    }
}