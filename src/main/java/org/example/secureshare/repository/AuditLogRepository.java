package org.example.secureshare.repository;

import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE al.userId = ?1")
    List<AuditLog> findAuditLogsByUserId(Long userId);

    @Query("SELECT al FROM AuditLog al WHERE al.userId = ?1")
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
}