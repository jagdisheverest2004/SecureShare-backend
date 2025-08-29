package org.example.secureshare.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FetchFileResponse {
    private String fileName;
    private String description;
    private String category;
    private String Data;
}
