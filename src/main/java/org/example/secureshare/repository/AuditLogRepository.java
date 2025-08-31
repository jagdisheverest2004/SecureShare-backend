package org.example.secureshare.repository;

import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUser(User user, Pageable pageable);

    List<AuditLog> findByUser_UserId(Long userId);
}