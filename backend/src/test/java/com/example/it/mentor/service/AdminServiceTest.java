package com.example.it.mentor.service;

import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.event.audit.RoleChangedAuditEvent;
import com.example.it.mentor.event.audit.UserStatusChangedAuditEvent;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock private UserService userService;
    @Mock private ApplicationEventPublisher eventPublisher;

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

        User admin = User.builder().email("admin@test.com").build();
        ReflectionTestUtils.setField(admin, "id", 99L);

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleCode.MENTOR)).thenReturn(Optional.of(mentorRole));
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.assignRole(1L, RoleCode.MENTOR);

        ArgumentCaptor<MentorProfile> captor = ArgumentCaptor.forClass(MentorProfile.class);
        verify(mentorProfileRepository).save(captor.capture());
        MentorProfile created = captor.getValue();
        assertThat(created.getFirstName()).isEqualTo("Иван");
        assertThat(created.getLastName()).isEqualTo("Иванов");
        assertThat(user.getRoles()).anyMatch(role -> role.getCode() == RoleCode.MENTOR);
        assertThat(user.getRoles()).noneMatch(role -> role.getCode() == RoleCode.STUDENT);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
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

        User admin = User.builder().email("admin@test.com").build();
        ReflectionTestUtils.setField(admin, "id", 99L);

        when(userRepository.findWithRolesById(2L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(mentorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(mentorProfile));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.assignRole(2L, RoleCode.STUDENT);

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(captor.capture());
        StudentProfile created = captor.getValue();
        assertThat(created.getFirstName()).isEqualTo("Анна");
        assertThat(created.getLastName()).isEqualTo("Смирнова");
        assertThat(user.getRoles()).anyMatch(role -> role.getCode() == RoleCode.STUDENT);
        assertThat(user.getRoles()).noneMatch(role -> role.getCode() == RoleCode.MENTOR);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(userDetailsService).evictUserCache("mentor@test.com");
    }

    @Test
    @DisplayName("assignRole — публикует RoleChangedAuditEvent с oldRole и newRole")
    void assignRole_publishesRoleChangedAuditEvent_withOldAndNewRole() {
        User user = userWithRole(RoleCode.STUDENT, "user@test.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        Role mentorRole = role(RoleCode.MENTOR);

        User admin = User.builder().email("admin@test.com").build();
        ReflectionTestUtils.setField(admin, "id", 99L);

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleCode.MENTOR)).thenReturn(Optional.of(mentorRole));
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.assignRole(1L, RoleCode.MENTOR);

        ArgumentCaptor<RoleChangedAuditEvent> captor = ArgumentCaptor.forClass(RoleChangedAuditEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RoleChangedAuditEvent event = captor.getValue();
        assertThat(event.adminUserId()).isEqualTo(99L);
        assertThat(event.targetUserId()).isEqualTo(1L);
        assertThat(event.oldRole()).isEqualTo(RoleCode.STUDENT);
        assertThat(event.newRole()).isEqualTo(RoleCode.MENTOR);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
    }

    // ── changeUserStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("changeUserStatus_blocked_bumpsTokenVersionAndEvictsCache")
    void changeUserStatus_blocked_bumpsTokenVersionAndEvictsCache() {
        User user = userWithRoleAndStatus(RoleCode.STUDENT, "user@test.com", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 5L);
        User admin = buildAdmin(99L);

        when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(user));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.changeUserStatus(5L, UserStatus.BLOCKED);

        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(userDetailsService).evictUserCache("user@test.com");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changeUserStatus_deleted_setsDeletedFlagAndBumpsTokenVersion")
    void changeUserStatus_deleted_setsDeletedFlagAndBumpsTokenVersion() {
        User user = userWithRoleAndStatus(RoleCode.STUDENT, "user@test.com", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 5L);
        User admin = buildAdmin(99L);

        when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(user));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.changeUserStatus(5L, UserStatus.DELETED);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("changeUserStatus_deletedToActive_clearsDeletedFlag")
    void changeUserStatus_deletedToActive_clearsDeletedFlag() {
        User user = userWithRoleAndStatus(RoleCode.STUDENT, "user@test.com", UserStatus.DELETED);
        user.setDeleted(true);
        ReflectionTestUtils.setField(user, "id", 5L);
        User admin = buildAdmin(99L);

        when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(user));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.changeUserStatus(5L, UserStatus.ACTIVE);

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getTokenVersion()).isEqualTo(0L); // no bump on unblock
    }

    @Test
    @DisplayName("changeUserStatus_activeFromBlocked_doesNotBumpTokenVersion")
    void changeUserStatus_activeFromBlocked_doesNotBumpTokenVersion() {
        User user = userWithRoleAndStatus(RoleCode.STUDENT, "user@test.com", UserStatus.BLOCKED);
        ReflectionTestUtils.setField(user, "id", 5L);
        User admin = buildAdmin(99L);

        when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(user));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.changeUserStatus(5L, UserStatus.ACTIVE);

        assertThat(user.getTokenVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("changeUserStatus_sameStatus_throwsConflict")
    void changeUserStatus_sameStatus_throwsConflict() {
        User user = userWithRoleAndStatus(RoleCode.STUDENT, "user@test.com", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 5L);

        when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changeUserStatus(5L, UserStatus.ACTIVE))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("changeUserStatus_emailNotConfirmed_throwsBusinessRule")
    void changeUserStatus_emailNotConfirmed_throwsBusinessRule() {
        assertThatThrownBy(() -> service.changeUserStatus(5L, UserStatus.EMAIL_NOT_CONFIRMED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("changeUserStatus_adminTarget_throwsBusinessRule")
    void changeUserStatus_adminTarget_throwsBusinessRule() {
        User adminUser = userWithRoleAndStatus(RoleCode.ADMIN, "admin@test.com", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(adminUser, "id", 10L);

        when(userRepository.findWithRolesById(10L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> service.changeUserStatus(10L, UserStatus.BLOCKED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("changeUserStatus_publishesAuditEvent_withOldAndNewStatus")
    void changeUserStatus_publishesAuditEvent_withOldAndNewStatus() {
        User user = userWithRoleAndStatus(RoleCode.STUDENT, "user@test.com", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 5L);
        User admin = buildAdmin(99L);

        when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(user));
        when(userService.getCurrentUserEntity()).thenReturn(admin);

        service.changeUserStatus(5L, UserStatus.BLOCKED);

        ArgumentCaptor<UserStatusChangedAuditEvent> captor =
                ArgumentCaptor.forClass(UserStatusChangedAuditEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        UserStatusChangedAuditEvent event = captor.getValue();
        assertThat(event.adminUserId()).isEqualTo(99L);
        assertThat(event.targetUserId()).isEqualTo(5L);
        assertThat(event.oldStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(event.newStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    private User userWithRoleAndStatus(RoleCode code, String email, UserStatus status) {
        User user = User.builder()
                .email(email)
                .status(status)
                .roles(new HashSet<>(Set.of(role(code))))
                .build();
        return user;
    }

    private User buildAdmin(long id) {
        User admin = User.builder().email("admin@test.com").build();
        ReflectionTestUtils.setField(admin, "id", id);
        return admin;
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
