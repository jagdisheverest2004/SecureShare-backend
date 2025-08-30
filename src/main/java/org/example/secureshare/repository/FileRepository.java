package org.example.secureshare.repository;

import org.example.secureshare.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface FileRepository extends JpaRepository<File, Long> , JpaSpecificationExecutor<File> {
     Optional<File> findByFilename(String filename);

    List<File> findByOwnerUserId(Long userId);
}