package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий метаданных загруженных файлов.
 */
@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
