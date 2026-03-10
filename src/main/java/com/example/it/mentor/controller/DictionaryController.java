package com.example.it.mentor.controller;

import com.example.it.mentor.dto.dict.CityResponse;
import com.example.it.mentor.dto.dict.InteractionTypeResponse;
import com.example.it.mentor.dto.dict.LanguageResponse;
import com.example.it.mentor.dto.dict.SkillResponse;
import com.example.it.mentor.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return dictionaryService.getCities();
    }

    @GetMapping("/skills")
    public List<SkillResponse> skills() {
        return dictionaryService.getSkills();
    }

    @GetMapping("/languages")
    public List<LanguageResponse> languages() {
        return dictionaryService.getLanguages();
    }

    @GetMapping("/interaction-types")
    public List<InteractionTypeResponse> interactionTypes() {
        return dictionaryService.getInteractionTypes();
    }
}
