package com.example.it.mentor.repository;

import com.example.it.mentor.entity.dict.DictCity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictCityRepository extends JpaRepository<DictCity, Long> {

    List<DictCity> findByActiveTrueOrderByNameAsc();
}
