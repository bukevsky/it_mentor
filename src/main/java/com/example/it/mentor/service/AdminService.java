package com.example.it.mentor.service;

import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public void assignRole(Long userId, RoleCode targetRole) {
        if (targetRole == RoleCode.ADMIN) {
            throw new BusinessRuleViolationException("Нельзя назначить роль ADMIN через этот endpoint");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));

        Role roleToAdd = roleRepository.findByCode(targetRole)
                .orElseThrow(() -> new NotFoundException("Роль не найдена: " + targetRole));

        RoleCode roleToRemove = targetRole == RoleCode.MENTOR ? RoleCode.STUDENT : RoleCode.MENTOR;
        user.getRoles().removeIf(r -> r.getCode() == roleToRemove);
        user.getRoles().removeIf(r -> r.getCode() == targetRole);
        user.getRoles().add(roleToAdd);
        userRepository.save(user);

        if (targetRole == RoleCode.MENTOR) {
            if (mentorProfileRepository.findByUserId(userId).isEmpty()) {
                String firstName = "";
                String lastName = "";
                Optional<StudentProfile> sp = studentProfileRepository.findByUserId(userId);
                if (sp.isPresent()) {
                    firstName = sp.get().getFirstName();
                    lastName = sp.get().getLastName();
                }
                mentorProfileRepository.save(MentorProfile.builder()
                        .user(user)
                        .firstName(firstName)
                        .lastName(lastName)
                        .build());
            }
        } else {
            if (studentProfileRepository.findByUserId(userId).isEmpty()) {
                String firstName = "";
                String lastName = "";
                Optional<MentorProfile> mp = mentorProfileRepository.findByUserId(userId);
                if (mp.isPresent()) {
                    firstName = mp.get().getFirstName();
                    lastName = mp.get().getLastName();
                }
                studentProfileRepository.save(StudentProfile.builder()
                        .user(user)
                        .firstName(firstName)
                        .lastName(lastName)
                        .build());
            }
        }

        userDetailsService.evictUserCache(user.getEmail());
    }
}
