package org.example.secureshare.repository;

import org.example.secureshare.model.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SharedFileRepository extends JpaRepository<SharedFile, Long>, JpaSpecificationExecutor<SharedFile> {
    List<SharedFile> findBySenderUserIdAndOriginalFileId(Long userId, Long id);

    List<SharedFile> findBySenderUserIdAndOriginalFileIdAndRecipientUsernameIn(Long userId, Long id, List<String> recipientUsernames);
}