package org.example.secureshare.controller;

import org.example.secureshare.payload.FetchFileResponse;
import org.example.secureshare.payload.UploadFileResponse;
import org.example.secureshare.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload/{file}")
    public ResponseEntity<UploadFileResponse> uploadFile(
            @PathVariable("file") MultipartFile file,
            @RequestParam("filename") String fileName,
            @RequestParam("description") String description,
            @RequestParam("category") String category) {
        try {
            Long fileId = fileService.storeFile(file, fileName, description, category);
            UploadFileResponse response = new UploadFileResponse(fileId, fileName, "File uploaded successfully!");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new UploadFileResponse(null, null,"Failed to upload file."));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<FetchFileResponse> fetchFile(@RequestParam("filename") String filename) {
        try {
            FetchFileResponse response = fileService.getFileByFilename(filename);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}