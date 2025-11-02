package org.example.secureshare.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "BYTEA")
    private byte[] encryptedData;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String encryptedAesKey;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String iv;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String authTag;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String signature;

    private String filename;
    private String description;
    private String category;
    private String contentType;
    private LocalDateTime timestamp;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "original_file_id")
    private Long originalFileId;

//    public File(byte[] encryptedData,String signature, String encryptedAesKey, String iv, String authTag,String filename, String description, String category, String contentType, Long ownerId) {
//        this.encryptedData = encryptedData;
//        this.encryptedAesKey = encryptedAesKey;
//        this.signature=signature;
//        this.iv = iv;
//        this.authTag=authTag;
//        this.filename = filename;
//        this.description = description;
//        this.category = category;
//        this.contentType = contentType;
//        this.ownerId = ownerId;
//        this.timestamp = LocalDateTime.now();
//    }

    public File(byte[] encryptedData, String signature, String encryptedAesKey, String iv,String authTag, String filename, String description, String category, String contentType, Long owner, Long originalFile) {
        this.encryptedData = encryptedData;
        this.signature=signature;
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.authTag=authTag;
        this.filename = filename;
        this.description = description;
        this.category = category;
        this.contentType = contentType;
        this.ownerId = owner;
        this.originalFileId = originalFile;
        this.timestamp = LocalDateTime.now();
    }
}