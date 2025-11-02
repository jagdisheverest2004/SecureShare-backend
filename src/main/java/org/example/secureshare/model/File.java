package org.example.secureshare.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "BYTEA")
    @JdbcTypeCode(Types.BINARY)
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

}