package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий справочника языков.
 */
public interface DictLanguageRepository extends JpaRepository<DictLanguage, Long> {

    /**
     * Возвращает активные языки, отсортированные по имени.
     *
     * @return список активных языков
     */
    List<DictLanguage> findByActiveTrueOrderByNameAsc();

    List<DictLanguage> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByCodeAndActiveTrue(String code);
}
