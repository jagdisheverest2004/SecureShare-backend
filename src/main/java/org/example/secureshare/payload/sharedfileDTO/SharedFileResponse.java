package org.example.secureshare.payload.sharedfileDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SharedFileResponse {
    private String senderName;
    private String recipientName;
    private String filename;
    private String category;
    private Boolean isSensitive;
    private LocalDateTime sharedAt;
}