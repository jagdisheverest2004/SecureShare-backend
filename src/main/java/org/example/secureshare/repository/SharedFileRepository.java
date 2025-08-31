package org.example.secureshare.repository;

import org.example.secureshare.model.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SharedFileRepository extends JpaRepository<SharedFile, Long>, JpaSpecificationExecutor<SharedFile> {

    // Correct method to find shared files by the original file's ID
    List<SharedFile> findByOriginalFile_Id(Long originalFileId);

    // Method for "by me" sharing to find files sent by a user
    List<SharedFile> findBySender_UserId(Long userId);

    // Method for "to me" sharing to find files sent to a user
    List<SharedFile> findByRecipient_UserId(Long userId);

    // Correct method for the "list" deletion type
    List<SharedFile> findByOriginalFile_IdAndRecipient_UsernameIn(Long originalFileId, List<String> usernames);

    // Correct method to delete all shared files of an original file and sender
    void deleteAllByOriginalFile_IdAndSender_UserId(Long originalFileId, Long senderId);
}