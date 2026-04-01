package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictSkillRepository extends JpaRepository<DictSkill, Long> {

    List<DictSkill> findByActiveTrueOrderByNameAsc();
}
