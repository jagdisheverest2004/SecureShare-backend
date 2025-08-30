package org.example.secureshare.repository;

import org.example.secureshare.model.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SharedFileRepository extends JpaRepository<SharedFile, Long>, JpaSpecificationExecutor<SharedFile> {

    // Find all sharing logs for a specific file sent by a user
    List<SharedFile> findBySenderUserIdAndFileId(Long senderId, Long fileId);

    // Find shared files for a specific file and a list of recipients
    List<SharedFile> findBySenderUserIdAndFileIdAndRecipientUsernameIn(Long senderId, Long fileId, List<String> recipientUsernames);
}