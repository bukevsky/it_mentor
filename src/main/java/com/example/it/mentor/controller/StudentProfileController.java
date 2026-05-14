package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.StudentFilesResponse;
import com.example.it.mentor.dto.student.PatchStudentProfileRequest;
import com.example.it.mentor.dto.student.PutStudentLanguagesRequest;
import com.example.it.mentor.dto.student.PutStudentSkillsRequest;
import com.example.it.mentor.dto.student.StudentCompletionResponse;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.dto.student.StudentSearchFilter;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.WorkFormat;
import com.example.it.mentor.service.FileStorage;
import com.example.it.mentor.service.StudentProfileService;
import com.example.it.mentor.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Student Profile", description = "Профиль студента")
public class StudentProfileController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "firstName", "lastName", "desiredPosition");

    private final StudentProfileService studentProfileService;
    private final FileStorage fileStorage;
    private final UserService userService;

    @PutMapping("/profile/student")
    public StudentProfileResponse upsert(@Valid @RequestBody StudentProfileRequest request) {
        return studentProfileService.upsertProfile(request);
    }

    @PatchMapping("/profile/student")
    public StudentProfileResponse patch(@Valid @RequestBody PatchStudentProfileRequest request) {
        return studentProfileService.patchProfile(request);
    }

    @GetMapping("/profile/student/me")
    public StudentProfileResponse myProfile() {
        return studentProfileService.getMyProfile();
    }

    @GetMapping("/profile/student/me/completion")
    public StudentCompletionResponse completion() {
        return studentProfileService.getCompletion();
    }

    @PutMapping("/profile/student/skills")
    public StudentProfileResponse replaceSkills(@Valid @RequestBody PutStudentSkillsRequest request) {
        return studentProfileService.replaceStudentSkills(request);
    }

    @PutMapping("/profile/student/languages")
    public StudentProfileResponse replaceLanguages(@Valid @RequestBody PutStudentLanguagesRequest request) {
        return studentProfileService.replaceStudentLanguages(request);
    }

    @GetMapping("/profile/student/me/files")
    public StudentFilesResponse getStudentFiles() {
        User user = userService.getCurrentUserEntity();
        return fileStorage.getStudentFiles(user.getId());
    }

    @GetMapping("/profiles/students/{id}")
    public StudentProfileResponse byId(@PathVariable Long id) {
        return studentProfileService.getProfileById(id);
    }

    @GetMapping("/profiles/students")
    public PagedResponse<StudentProfileResponse> searchStudents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) @Size(max = 20) List<Long> skillIds,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) WorkFormat workFormat,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        StudentSearchFilter filter = new StudentSearchFilter(q, cityId, skillIds, employmentType, workFormat);

        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        if (!ALLOWED_SORT_FIELDS.contains(field)) field = "createdAt";
        Sort.Direction dir = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        return studentProfileService.searchStudents(filter,
                PageRequest.of(page, size, Sort.by(dir, field)));
    }
}
