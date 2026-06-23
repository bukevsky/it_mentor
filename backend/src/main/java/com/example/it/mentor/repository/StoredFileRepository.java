package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.enums.FileStatus;
import com.example.it.mentor.entity.enums.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Page<StoredFile> findByOwnerIdAndStatus(Long ownerId, FileStatus status, Pageable pageable);

    Page<StoredFile> findByOwnerIdAndFileTypeAndStatus(Long ownerId, FileType fileType, FileStatus status, Pageable pageable);

    Optional<StoredFile> findFirstByOwnerIdAndFileTypeAndStatus(Long ownerId, FileType fileType, FileStatus status);

    List<StoredFile> findAllByOwnerIdAndFileTypeAndStatus(Long ownerId, FileType fileType, FileStatus status);

    long countByOwnerIdAndFileTypeAndStatus(Long ownerId, FileType fileType, FileStatus status);
}
