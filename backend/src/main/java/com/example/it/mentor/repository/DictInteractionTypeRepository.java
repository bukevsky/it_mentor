package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictInteractionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictInteractionTypeRepository extends JpaRepository<DictInteractionType, Long> {

    List<DictInteractionType> findByActiveTrueOrderByNameAsc();
}
