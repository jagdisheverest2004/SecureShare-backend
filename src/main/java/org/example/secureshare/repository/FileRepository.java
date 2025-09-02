package org.example.secureshare.repository;

import org.example.secureshare.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {

    @Query("SELECT f FROM File f WHERE f.ownerId = ?1")
    List<File> findByOwnerId(Long userId);
}