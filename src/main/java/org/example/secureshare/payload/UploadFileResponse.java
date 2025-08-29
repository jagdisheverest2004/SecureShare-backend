package org.example.secureshare.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileResponse {
    private Long fileId;
    private String fileName;
    private String message;
}