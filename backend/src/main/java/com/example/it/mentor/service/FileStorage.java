package com.example.it.mentor.service;

import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.entity.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    FileUploadResponse store(MultipartFile file, FileType type, Long ownerId);

    void delete(Long fileId, Long requesterId);

    void requireOwned(Long fileId, Long ownerId);
}
