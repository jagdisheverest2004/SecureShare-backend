package org.example.secureshare.payload.fiteDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FetchFileResponse {
    private Long id;
    private String filename;
    private String description;
    private String category;
    private LocalDateTime createdAt;
}
