package com.example.it.mentor.controller;

import com.example.it.mentor.dto.dict.*;
import com.example.it.mentor.service.AdminDictionaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dictionaries")
@RequiredArgsConstructor
@Tag(name = "admin-dictionaries", description = "CRUD управления справочниками")
public class AdminDictionaryController {

    private final AdminDictionaryService service;

    // === Cities ===

    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return service.listAllCities();
    }

    @PostMapping("/cities")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CreateCityRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCity(dto));
    }

    @PutMapping("/cities/{id}")
    public CityResponse updateCity(@PathVariable Long id, @Valid @RequestBody UpdateCityRequest dto) {
        return service.updateCity(id, dto);
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Void> deactivateCity(@PathVariable Long id) {
        service.deactivateCity(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cities/{id}/restore")
    public CityResponse restoreCity(@PathVariable Long id) {
        return service.restoreCity(id);
    }

    // === Skills ===

    @GetMapping("/skills")
    public List<SkillResponse> listSkills() {
        return service.listAllSkills();
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSkill(dto));
    }

    @PutMapping("/skills/{id}")
    public SkillResponse updateSkill(@PathVariable Long id, @Valid @RequestBody UpdateSkillRequest dto) {
        return service.updateSkill(id, dto);
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deactivateSkill(@PathVariable Long id) {
        service.deactivateSkill(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/skills/{id}/restore")
    public SkillResponse restoreSkill(@PathVariable Long id) {
        return service.restoreSkill(id);
    }

    // === Languages ===

    @GetMapping("/languages")
    public List<LanguageResponse> listLanguages() {
        return service.listAllLanguages();
    }

    @PostMapping("/languages")
    public ResponseEntity<LanguageResponse> createLanguage(@Valid @RequestBody CreateLanguageRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createLanguage(dto));
    }

    @PutMapping("/languages/{id}")
    public LanguageResponse updateLanguage(@PathVariable Long id, @Valid @RequestBody UpdateLanguageRequest dto) {
        return service.updateLanguage(id, dto);
    }

    @DeleteMapping("/languages/{id}")
    public ResponseEntity<Void> deactivateLanguage(@PathVariable Long id) {
        service.deactivateLanguage(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/languages/{id}/restore")
    public LanguageResponse restoreLanguage(@PathVariable Long id) {
        return service.restoreLanguage(id);
    }

    // === Interaction Types ===

    @GetMapping("/interaction-types")
    public List<InteractionTypeResponse> listInteractionTypes() {
        return service.listAllInteractionTypes();
    }

    @PostMapping("/interaction-types")
    public ResponseEntity<InteractionTypeResponse> createInteractionType(
            @Valid @RequestBody CreateInteractionTypeRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createInteractionType(dto));
    }

    @PutMapping("/interaction-types/{id}")
    public InteractionTypeResponse updateInteractionType(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateInteractionTypeRequest dto) {
        return service.updateInteractionType(id, dto);
    }

    @DeleteMapping("/interaction-types/{id}")
    public ResponseEntity<Void> deactivateInteractionType(@PathVariable Long id) {
        service.deactivateInteractionType(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/interaction-types/{id}/restore")
    public InteractionTypeResponse restoreInteractionType(@PathVariable Long id) {
        return service.restoreInteractionType(id);
    }
}
