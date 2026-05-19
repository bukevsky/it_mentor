package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictCity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий справочника городов.
 */
public interface DictCityRepository extends JpaRepository<DictCity, Long> {

    /**
     * Возвращает активные города, отсортированные по имени.
     *
     * @return список активных городов
     */
    List<DictCity> findByActiveTrueOrderByNameAsc();

    List<DictCity> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
