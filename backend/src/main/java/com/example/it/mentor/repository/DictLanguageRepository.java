package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictLanguageRepository extends JpaRepository<DictLanguage, Long> {

    List<DictLanguage> findByActiveTrueOrderByNameAsc();
}
