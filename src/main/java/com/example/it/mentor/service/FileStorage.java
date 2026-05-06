package com.example.it.mentor.service;

import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.entity.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

/**
 * Контракт файлового хранилища пользовательских загрузок.
 */
public interface FileStorage {

    /**
     * Сохраняет файл в объектном хранилище и регистрирует его метаданные в базе данных.
     *
     * @param file загружаемый файл
     * @param type тип файла с правилами валидации
     * @param ownerId идентификатор владельца файла
     * @return метаданные сохранённого файла
     */
    FileUploadResponse store(MultipartFile file, FileType type, Long ownerId);

    /**
     * Удаляет файл из физического и метаданных хранилищ.
     *
     * @param fileId идентификатор файла
     * @param requesterId идентификатор пользователя, инициировавшего удаление
     */
    void delete(Long fileId, Long requesterId);

    /**
     * Проверяет, что файл принадлежит указанному пользователю.
     *
     * @param fileId идентификатор файла
     * @param ownerId идентификатор ожидаемого владельца
     */
    void requireOwned(Long fileId, Long ownerId);
}
