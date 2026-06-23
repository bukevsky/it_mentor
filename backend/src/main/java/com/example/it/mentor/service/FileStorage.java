package com.example.it.mentor.service;

import com.example.it.mentor.dto.FileDownloadInfo;
import com.example.it.mentor.dto.FileResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.StudentFilesResponse;
import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.entity.enums.FileType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    FileUploadResponse store(MultipartFile file, FileType type, Long ownerId);

    void delete(Long fileId, Long requesterId);

    void requireOwned(Long fileId, Long ownerId);

    PagedResponse<FileResponse> listFiles(FileType type, Long ownerId, Pageable pageable);

    FileDownloadInfo download(Long fileId, Long requesterId);

    void softDeleteFile(Long fileId, Long requesterId);

    FileResponse replaceFile(Long fileId, MultipartFile newFile, Long ownerId);

    StudentFilesResponse getStudentFiles(Long userId);
}
