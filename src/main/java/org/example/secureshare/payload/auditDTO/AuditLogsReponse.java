package org.example.secureshare.payload.auditDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.secureshare.model.AuditLog;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogsReponse {
    List<AuditLog> auditLogList;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
    private boolean lastPage;
}
