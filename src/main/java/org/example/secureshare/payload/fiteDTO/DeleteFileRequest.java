package org.example.secureshare.payload.fiteDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteFileRequest {
    private String deletionType; // "me", "everyone", "list"
    private List<String> recipientUsernames;
}