package org.example.secureshare.repository;

import org.example.secureshare.model.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SharedFileRepository extends JpaRepository<SharedFile, Long>, JpaSpecificationExecutor<SharedFile> {

    @Query("SELECT sf FROM SharedFile sf WHERE sf.originalFileId = ?1")
    List<SharedFile> findSharedFilesByFileId(Long fileId);

    @Query("SELECT sf FROM SharedFile sf WHERE sf.originalFileId = ?1 AND sf.recipientId IN ?2")
    List<SharedFile> findSharedFilesByFileIdAndRecipientId(Long fileId, List<Long> recipientIds);

    @Modifying
    @Query("Delete FROM SharedFile sf WHERE sf.newFileId = ?1")
    void deleteByNewFileId(Long fileId);
}