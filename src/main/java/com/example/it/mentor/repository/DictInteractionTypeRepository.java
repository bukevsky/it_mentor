package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictInteractionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий справочника типов взаимодействия.
 */
public interface DictInteractionTypeRepository extends JpaRepository<DictInteractionType, Long> {

    /**
     * Возвращает активные типы взаимодействия, отсортированные по имени.
     *
     * @return список активных типов взаимодействия
     */
    List<DictInteractionType> findByActiveTrueOrderByNameAsc();

    List<DictInteractionType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
