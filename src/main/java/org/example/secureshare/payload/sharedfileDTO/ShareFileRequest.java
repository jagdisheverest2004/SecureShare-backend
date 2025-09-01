package org.example.secureshare.payload.sharedfileDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareFileRequest {
    private String recipientUsername;
    private Long fileId;
    private Boolean isSensitive;
}