package com.example.it.mentor.service;

import com.example.it.mentor.dto.notification.NotificationPreferencesResponse;
import com.example.it.mentor.dto.notification.UpdateNotificationPreferencesRequest;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.UserNotificationPreferences;
import com.example.it.mentor.repository.UserNotificationPreferencesRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferencesService")
class NotificationPreferencesServiceTest {

    @InjectMocks private NotificationPreferencesService service;

    @Mock private UserNotificationPreferencesRepository prefsRepository;
    @Mock private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().build();
        ReflectionTestUtils.setField(currentUser, "id", 1L);
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken("user@test.com", null, List.of())));
        lenient().when(userService.getCurrentUserEntity()).thenReturn(currentUser);
        lenient().when(prefsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("getForCurrentUser")
    class GetForCurrentUser {

        @Test
        void getForCurrentUser_noRecord_createsDefaultsAllTrue() {
            when(prefsRepository.findByUserId(1L)).thenReturn(Optional.empty());

            NotificationPreferencesResponse response = service.getForCurrentUser();

            assertThat(response.emailRequestEvents()).isTrue();
            assertThat(response.emailSessionEvents()).isTrue();
            assertThat(response.emailReviewEvents()).isTrue();
            verify(prefsRepository).save(any(UserNotificationPreferences.class));
        }

        @Test
        void getForCurrentUser_existing_returnsStoredValues() {
            UserNotificationPreferences prefs = UserNotificationPreferences.builder()
                    .user(currentUser)
                    .emailRequestEvents(false)
                    .emailSessionEvents(true)
                    .emailReviewEvents(false)
                    .build();
            when(prefsRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            NotificationPreferencesResponse response = service.getForCurrentUser();

            assertThat(response.emailRequestEvents()).isFalse();
            assertThat(response.emailSessionEvents()).isTrue();
            assertThat(response.emailReviewEvents()).isFalse();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void update_partialFields_appliesOnlyProvided() {
            UserNotificationPreferences prefs = UserNotificationPreferences.builder()
                    .user(currentUser)
                    .emailRequestEvents(true)
                    .emailSessionEvents(true)
                    .emailReviewEvents(true)
                    .build();
            when(prefsRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            service.update(new UpdateNotificationPreferencesRequest(false, null, null));

            ArgumentCaptor<UserNotificationPreferences> captor = ArgumentCaptor.forClass(UserNotificationPreferences.class);
            verify(prefsRepository).save(captor.capture());
            UserNotificationPreferences saved = captor.getValue();
            assertThat(saved.isEmailRequestEvents()).isFalse();
            assertThat(saved.isEmailSessionEvents()).isTrue();
            assertThat(saved.isEmailReviewEvents()).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldNotify")
    class ShouldNotify {

        @Test
        void shouldNotify_noRecord_returnsTrue() {
            when(prefsRepository.findByUserId(1L)).thenReturn(Optional.empty());
            assertThat(service.shouldNotify(1L, "request")).isTrue();
        }

        @Test
        void shouldNotify_requestDisabled_returnsFalse() {
            UserNotificationPreferences prefs = UserNotificationPreferences.builder()
                    .user(currentUser)
                    .emailRequestEvents(false)
                    .build();
            when(prefsRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            assertThat(service.shouldNotify(1L, "request")).isFalse();
        }
    }
}
