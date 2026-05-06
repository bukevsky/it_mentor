package com.example.it.mentor.service;

import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService")
class AdminServiceTest {

    @InjectMocks
    private AdminService service;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("assignRole to MENTOR without mentor profile creates profile from student names and evicts cache")
    void assignRole_mentorWithoutProfile_shouldBootstrapMentorProfile() {
        User user = userWithRole(RoleCode.STUDENT, "user@test.com");
        Role mentorRole = role(RoleCode.MENTOR);
        StudentProfile studentProfile = StudentProfile.builder()
                .user(user)
                .firstName("Иван")
                .lastName("Иванов")
                .build();

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleCode.MENTOR)).thenReturn(Optional.of(mentorRole));
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));

        service.assignRole(1L, RoleCode.MENTOR);

        ArgumentCaptor<MentorProfile> captor = ArgumentCaptor.forClass(MentorProfile.class);
        verify(mentorProfileRepository).save(captor.capture());
        MentorProfile created = captor.getValue();
        assertThat(created.getFirstName()).isEqualTo("Иван");
        assertThat(created.getLastName()).isEqualTo("Иванов");
        assertThat(user.getRoles()).anyMatch(role -> role.getCode() == RoleCode.MENTOR);
        assertThat(user.getRoles()).noneMatch(role -> role.getCode() == RoleCode.STUDENT);
        verify(userDetailsService).evictUserCache("user@test.com");
    }

    @Test
    @DisplayName("assignRole to STUDENT without student profile creates profile from mentor names and evicts cache")
    void assignRole_studentWithoutProfile_shouldBootstrapStudentProfile() {
        User user = userWithRole(RoleCode.MENTOR, "mentor@test.com");
        Role studentRole = role(RoleCode.STUDENT);
        MentorProfile mentorProfile = MentorProfile.builder()
                .user(user)
                .firstName("Анна")
                .lastName("Смирнова")
                .build();

        when(userRepository.findWithRolesById(2L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(mentorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(mentorProfile));

        service.assignRole(2L, RoleCode.STUDENT);

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(captor.capture());
        StudentProfile created = captor.getValue();
        assertThat(created.getFirstName()).isEqualTo("Анна");
        assertThat(created.getLastName()).isEqualTo("Смирнова");
        assertThat(user.getRoles()).anyMatch(role -> role.getCode() == RoleCode.STUDENT);
        assertThat(user.getRoles()).noneMatch(role -> role.getCode() == RoleCode.MENTOR);
        verify(userDetailsService).evictUserCache("mentor@test.com");
    }

    private User userWithRole(RoleCode code, String email) {
        return User.builder()
                .email(email)
                .roles(new HashSet<>(Set.of(role(code))))
                .build();
    }

    private Role role(RoleCode code) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        ReflectionTestUtils.setField(role, "name", code.name());
        return role;
    }
}
