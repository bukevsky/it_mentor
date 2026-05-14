package com.example.it.mentor.service;

import com.example.it.mentor.dto.presence.PresenceResponse;
import com.example.it.mentor.dto.sse.PresencePayload;
import com.example.it.mentor.entity.UserPresence;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.ChatRepository;
import com.example.it.mentor.repository.UserPresenceRepository;
import com.example.it.mentor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceService")
class PresenceServiceTest {

    @InjectMocks private PresenceService service;

    @Mock private UserPresenceRepository presenceRepository;
    @Mock private ChatRepository chatRepository;
    @Mock private ChatSseService sseService;
    @Mock private UserRepository userRepository;

    private static final Long USER_ID = 7L;
    private static final Long PARTNER_ID = 8L;

    @BeforeEach
    void setUp() {
        // lenient — этот stub нужен только setOnline/setOffline тестам, getPresence его не вызывает
        lenient().when(chatRepository.findAllChatPartnerIds(USER_ID)).thenReturn(List.of(PARTNER_ID));
    }

    @Test
    @DisplayName("setOnline рассылает presence.changed(online) собеседникам")
    void setOnline_happyPath_shouldBroadcastOnlineToPartners() {
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.setOnline(USER_ID);

        ArgumentCaptor<PresencePayload> captor = ArgumentCaptor.forClass(PresencePayload.class);
        verify(sseService).pushPresenceChanged(eq(Set.of(PARTNER_ID)), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().status()).isEqualTo("online");
    }

    @Test
    @DisplayName("setOnline без собеседников не вызывает pushPresenceChanged")
    void setOnline_noPartners_shouldNotCallPushPresence() {
        when(chatRepository.findAllChatPartnerIds(USER_ID)).thenReturn(List.of());
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.setOnline(USER_ID);

        verify(sseService).pushPresenceChanged(eq(Set.of()), any(PresencePayload.class));
    }

    @Test
    @DisplayName("setOffline сохраняет lastSeenAt и рассылает presence.changed(offline)")
    void setOffline_happyPath_shouldPersistLastSeenAtAndBroadcastOffline() {
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.setOffline(USER_ID);

        verify(presenceRepository).save(argThat(p -> p.getLastSeenAt() != null));
        ArgumentCaptor<PresencePayload> captor = ArgumentCaptor.forClass(PresencePayload.class);
        verify(sseService).pushPresenceChanged(eq(Set.of(PARTNER_ID)), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("offline");
    }

    @Test
    @DisplayName("setOffline создаёт новую строку если нет существующей записи")
    void setOffline_noExistingPresenceRow_shouldCreateNewRow() {
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.setOffline(USER_ID);

        verify(presenceRepository).save(argThat(p ->
                p.getUserId().equals(USER_ID) && p.getLastSeenAt() != null));
    }

    @Test
    @DisplayName("setOffline обновляет lastSeenAt существующей записи")
    void setOffline_existingPresenceRow_shouldUpdateLastSeenAt() {
        OffsetDateTime oldTime = OffsetDateTime.now().minusHours(1);
        UserPresence existing = UserPresence.builder()
                .userId(USER_ID)
                .lastSeenAt(oldTime)
                .build();
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        service.setOffline(USER_ID);

        verify(presenceRepository).save(argThat(p -> p.getLastSeenAt().isAfter(oldTime)));
    }

    @Test
    @DisplayName("getPresence возвращает online если пользователь подключён")
    void getPresence_userIsOnline_shouldReturnOnlineStatus() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sseService.isOnline(USER_ID)).thenReturn(true);
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        PresenceResponse result = service.getPresence(USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.status()).isEqualTo("online");
        assertThat(result.lastSeenAt()).isNull();
    }

    @Test
    @DisplayName("getPresence возвращает offline если пользователь не подключён")
    void getPresence_userIsOffline_shouldReturnOfflineStatus() {
        OffsetDateTime lastSeen = OffsetDateTime.now().minusMinutes(5);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sseService.isOnline(USER_ID)).thenReturn(false);
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                UserPresence.builder().userId(USER_ID).lastSeenAt(lastSeen).build()));

        PresenceResponse result = service.getPresence(USER_ID);

        assertThat(result.status()).isEqualTo("offline");
        assertThat(result.lastSeenAt()).isEqualTo(lastSeen);
    }

    @Test
    @DisplayName("getPresence возвращает null lastSeenAt если нет записи в БД")
    void getPresence_noPresenceRow_shouldReturnNullLastSeenAt() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sseService.isOnline(USER_ID)).thenReturn(false);
        when(presenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        PresenceResponse result = service.getPresence(USER_ID);

        assertThat(result.lastSeenAt()).isNull();
    }

    @Test
    @DisplayName("getPresence выбрасывает NotFoundException если пользователь не найден")
    void getPresence_userNotFound_shouldThrowNotFoundException() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getPresence(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(sseService, never()).isOnline(any());
    }
}
