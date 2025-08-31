package org.example.secureshare.repository;

import org.example.secureshare.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {

    // Correct method to find shared files by a user, but exclude the owner's original file
    List<File> findByOriginalFile_IdAndOwner_UserIdNot(Long originalFileId, Long ownerId);

    // Correct method to find shared files for a specific list of recipients
    List<File> findByOriginalFile_IdAndOwner_UsernameIn(Long originalFileId, List<String> recipientUsernames);

    // Existing method
    List<File> findByOwner_UserId(Long userId);

}