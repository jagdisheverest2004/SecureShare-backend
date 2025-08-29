package org.example.secureshare.service;

import org.example.secureshare.model.File;
import org.example.secureshare.payload.FetchFileResponse;
import org.example.secureshare.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    public Long storeFile(MultipartFile file, String fileName,String description, String category) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());

        File newFile = new File();
        newFile.setFilename(fileName);
        newFile.setDescription(description);
        newFile.setCategory(category);
        newFile.setBase64Data(base64Data);
        newFile.setTimestamp(LocalDateTime.now());

        File savedFile = fileRepository.save(newFile);
        return savedFile.getId();
    }

    public FetchFileResponse getFileByFilename(String filename) {
        File file = fileRepository.findByFilename(filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found with filename: " + filename));
        return new FetchFileResponse(file.getFilename(), file.getDescription(), file.getCategory(), file.getBase64Data());
    }
}