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
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] encryptedData;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String encryptedAesKey;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String iv; // Initialization Vector for GCM

    private String filename;
    private String description;
    private String category;
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    // Custom constructor to avoid circular dependency with Lombok's @AllArgsConstructor
    public File(byte[] encryptedData, String encryptedAesKey, String iv, String filename, String description, String category, User owner) {
        this.encryptedData = encryptedData;
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.filename = filename;
        this.description = description;
        this.category = category;
        this.owner = owner;
        this.timestamp = LocalDateTime.now();
    }


}