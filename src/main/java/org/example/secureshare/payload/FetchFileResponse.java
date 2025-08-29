package org.example.secureshare.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FetchFileResponse {
    private Long id;
    private String fileName;
    private String description;
    private String category;
}
