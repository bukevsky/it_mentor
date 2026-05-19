package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.admin.AdminUserResponse;
import com.example.it.mentor.dto.admin.AdminUsersStatsResponse;
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
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import com.example.it.mentor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void assignRole(Long userId, RoleCode targetRole) {
        log.info("Назначение роли пользователю: userId={}, targetRole={}", userId, targetRole);
        validateTargetRole(targetRole);
        User user = loadUser(userId);
        Role roleToAdd = loadRole(targetRole);
        RoleCode roleToRemove = targetRole == RoleCode.MENTOR ? RoleCode.STUDENT : RoleCode.MENTOR;
        RoleCode oldRole = user.getRoles().stream()
                .map(Role::getCode)
                .filter(c -> c == roleToRemove)
                .findFirst()
                .orElse(roleToRemove);
        replaceRole(user, roleToAdd, targetRole, roleToRemove);
        userRepository.save(user);
        log.info("Роль назначена: userId={}, newRole={}, removedRole={}", userId, targetRole, roleToRemove);
        ensureProfileExists(user, userId, targetRole);
        userDetailsService.evictUserCache(user.getEmail());
        Long adminId = userService.getCurrentUserEntity().getId();
        eventPublisher.publishEvent(new RoleChangedAuditEvent(adminId, userId, oldRole, targetRole));
    }

    @Transactional
    public void changeUserStatus(Long userId, UserStatus newStatus) {
        log.info("Смена статуса пользователя: userId={}, newStatus={}", userId, newStatus);

        if (newStatus == UserStatus.EMAIL_NOT_CONFIRMED) {
            throw new BusinessRuleViolationException(
                    "Статус EMAIL_NOT_CONFIRMED управляется только системой");
        }

        User user = loadUser(userId);
        UserStatus oldStatus = user.getStatus();

        if (oldStatus == newStatus) {
            throw new ConflictException("Статус уже установлен: " + newStatus);
        }

        boolean isAdmin = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(c -> c == RoleCode.ADMIN);
        if (isAdmin) {
            throw new BusinessRuleViolationException("Нельзя менять статус администратора");
        }

        if (newStatus == UserStatus.DELETED) {
            user.setDeleted(true);
        } else if (oldStatus == UserStatus.DELETED) {
            user.setDeleted(false);
        }

        if (newStatus == UserStatus.BLOCKED || newStatus == UserStatus.DELETED) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }

        user.setStatus(newStatus);
        userRepository.save(user);
        log.info("Статус изменён: userId={}, oldStatus={}, newStatus={}, tokenVersion={}",
                userId, oldStatus, newStatus, user.getTokenVersion());

        userDetailsService.evictUserCache(user.getEmail());

        Long adminId = userService.getCurrentUserEntity().getId();
        eventPublisher.publishEvent(new UserStatusChangedAuditEvent(adminId, userId, oldStatus, newStatus));
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> getUsers(String q, RoleCode role, UserStatus status, Pageable pageable) {
        String qLower = (q != null && !q.isBlank()) ? q.toLowerCase() : null;
        Page<User> page = userRepository.searchUsers(qLower, status, role, pageable);

        List<Long> userIds = page.getContent().stream().map(User::getId).toList();
        Map<Long, StudentProfile> studentMap = studentProfileRepository.findAllByUserIdIn(userIds).stream()
                .filter(sp -> sp.getUser() != null)
                .collect(Collectors.toMap(sp -> sp.getUser().getId(), Function.identity()));
        Map<Long, MentorProfile> mentorMap = mentorProfileRepository.findAllByUserIdIn(userIds).stream()
                .filter(mp -> mp.getUser() != null)
                .collect(Collectors.toMap(mp -> mp.getUser().getId(), Function.identity()));

        return PagedResponse.from(page.map(user -> toAdminUserResponse(user, studentMap, mentorMap)));
    }

    @Transactional(readOnly = true)
    public AdminUsersStatsResponse getUsersStats() {
        long total = userRepository.countByDeletedFalse();

        Map<String, Long> byRole = new HashMap<>();
        for (Object[] row : userRepository.countByRoleRaw()) {
            byRole.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : userRepository.countByStatusRaw()) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }

        return new AdminUsersStatsResponse(total, byRole, byStatus);
    }

    private AdminUserResponse toAdminUserResponse(User user,
                                                   Map<Long, StudentProfile> studentMap,
                                                   Map<Long, MentorProfile> mentorMap) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .collect(Collectors.toList());

        String firstName = null;
        String lastName = null;

        StudentProfile sp = studentMap.get(user.getId());
        if (sp != null) {
            firstName = sp.getFirstName();
            lastName = sp.getLastName();
        } else {
            MentorProfile mp = mentorMap.get(user.getId());
            if (mp != null) {
                firstName = mp.getFirstName();
                lastName = mp.getLastName();
            }
        }

        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().name(),
                roles,
                firstName,
                lastName,
                user.getCreatedAt()
        );
    }

    private void validateTargetRole(RoleCode targetRole) {
        if (targetRole == RoleCode.ADMIN) {
            throw new BusinessRuleViolationException("Нельзя назначить роль ADMIN через этот endpoint");
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
    }

    private Role loadRole(RoleCode targetRole) {
        return roleRepository.findByCode(targetRole)
                .orElseThrow(() -> new NotFoundException("Роль не найдена: " + targetRole));
    }

    private void replaceRole(User user, Role roleToAdd, RoleCode targetRole, RoleCode roleToRemove) {
        user.getRoles().removeIf(role -> role.getCode() == roleToRemove || role.getCode() == targetRole);
        user.getRoles().add(roleToAdd);
    }

    private void ensureProfileExists(User user, Long userId, RoleCode targetRole) {
        if (targetRole == RoleCode.MENTOR) {
            ensureMentorProfileExists(user, userId);
            return;
        }
        ensureStudentProfileExists(user, userId);
    }

    private void ensureMentorProfileExists(User user, Long userId) {
        if (mentorProfileRepository.findByUserId(userId).isPresent()) return;

        ProfileNames names = studentProfileRepository.findByUserId(userId)
                .map(this::toProfileNames)
                .orElse(ProfileNames.empty());

        mentorProfileRepository.save(MentorProfile.builder()
                .user(user).firstName(names.firstName()).lastName(names.lastName()).build());
        log.debug("Создан профиль ментора при смене роли: userId={}", userId);
    }

    private void ensureStudentProfileExists(User user, Long userId) {
        if (studentProfileRepository.findByUserId(userId).isPresent()) return;

        ProfileNames names = mentorProfileRepository.findByUserId(userId)
                .map(this::toProfileNames)
                .orElse(ProfileNames.empty());

        studentProfileRepository.save(StudentProfile.builder()
                .user(user).firstName(names.firstName()).lastName(names.lastName()).build());
        log.debug("Создан профиль студента при смене роли: userId={}", userId);
    }

    private ProfileNames toProfileNames(StudentProfile profile) {
        return new ProfileNames(profile.getFirstName(), profile.getLastName());
    }

    private ProfileNames toProfileNames(MentorProfile profile) {
        return new ProfileNames(profile.getFirstName(), profile.getLastName());
    }

    private record ProfileNames(String firstName, String lastName) {
        private static ProfileNames empty() { return new ProfileNames("", ""); }
    }
}
