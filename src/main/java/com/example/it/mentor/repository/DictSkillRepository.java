package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий справочника навыков.
 */
public interface DictSkillRepository extends JpaRepository<DictSkill, Long> {

    /**
     * Возвращает активные навыки, отсортированные по имени.
     *
     * @return список активных навыков
     */
    List<DictSkill> findByActiveTrueOrderByNameAsc();
}
