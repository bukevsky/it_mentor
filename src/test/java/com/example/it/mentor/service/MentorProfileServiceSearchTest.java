package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentor.MentorCardResponse;
import com.example.it.mentor.dto.mentor.MentorSearchFilter;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.mapper.MentorProfileMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentorSkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorProfileService#searchMentors")
class MentorProfileServiceSearchTest {

    @InjectMocks
    private MentorProfileService service;

    @Mock private MentorProfileRepository profileRepository;
    @Mock private MentorSkillRepository skillRepository;
    @Mock private UserService userService;
    @Mock private DictCityRepository cityRepository;
    @Mock private DictSkillRepository skillRefRepository;
    @Mock private MentorProfileMapper mapper;

    private static final MentorSearchFilter EMPTY_FILTER =
            new MentorSearchFilter(null, null, null, null, null, null);

    @Nested
    @DisplayName("searchMentors")
    class SearchMentors {

        @Test
        @DisplayName("пустая БД — возвращает пустой PagedResponse без вызова Phase 2")
        void emptyDatabase_shouldReturnEmptyPagedResponse() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<MentorProfile> emptyPage = Page.empty(pageable);
            when(profileRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            PagedResponse<MentorCardResponse> result = service.searchMentors(EMPTY_FILTER, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(profileRepository, never()).findAllWithDetailsByIdIn(anyList());
        }

        @Test
        @DisplayName("найдены профили — Phase 2 загружает детали и возвращает карточки")
        void profilesFound_shouldLoadDetailsAndReturnCards() {
            Pageable pageable = PageRequest.of(0, 20);
            MentorProfile p1 = profileWithId(1L);
            MentorProfile p2 = profileWithId(2L);
            Page<MentorProfile> phase1Page = new PageImpl<>(List.of(p1, p2), pageable, 2);

            MentorProfile d1 = profileWithId(1L);
            MentorProfile d2 = profileWithId(2L);
            MentorCardResponse card1 = cardResponse(1L, "Алексей");
            MentorCardResponse card2 = cardResponse(2L, "Борис");

            when(profileRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(phase1Page);
            when(profileRepository.findAllWithDetailsByIdIn(List.of(1L, 2L))).thenReturn(List.of(d1, d2));
            when(mapper.toCardResponse(d1)).thenReturn(card1);
            when(mapper.toCardResponse(d2)).thenReturn(card2);

            PagedResponse<MentorCardResponse> result = service.searchMentors(EMPTY_FILTER, pageable);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            verify(profileRepository).findAllWithDetailsByIdIn(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Phase 2 возвращает элементы не в том порядке — итоговый список сохраняет порядок Phase 1")
        void searchMentors_orderPreserved_whenPhase2ReturnsOutOfOrder() {
            Pageable pageable = PageRequest.of(0, 3);
            MentorProfile p5 = profileWithId(5L);
            MentorProfile p3 = profileWithId(3L);
            MentorProfile p1 = profileWithId(1L);
            Page<MentorProfile> phase1Page = new PageImpl<>(List.of(p5, p3, p1), pageable, 3);

            // Phase 2 возвращает в другом порядке: [1, 3, 5]
            MentorProfile d1 = profileWithId(1L);
            MentorProfile d3 = profileWithId(3L);
            MentorProfile d5 = profileWithId(5L);
            MentorCardResponse card5 = cardResponse(5L, "Е");
            MentorCardResponse card3 = cardResponse(3L, "Г");
            MentorCardResponse card1 = cardResponse(1L, "А");

            when(profileRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(phase1Page);
            when(profileRepository.findAllWithDetailsByIdIn(List.of(5L, 3L, 1L))).thenReturn(List.of(d1, d3, d5));
            when(mapper.toCardResponse(d5)).thenReturn(card5);
            when(mapper.toCardResponse(d3)).thenReturn(card3);
            when(mapper.toCardResponse(d1)).thenReturn(card1);

            PagedResponse<MentorCardResponse> result = service.searchMentors(EMPTY_FILTER, pageable);

            assertThat(result.content()).extracting(MentorCardResponse::id)
                    .containsExactly(5L, 3L, 1L);
        }
    }

    // helpers

    private static MentorProfile profileWithId(Long id) {
        MentorProfile profile = mock(MentorProfile.class);
        when(profile.getId()).thenReturn(id);
        return profile;
    }

    private static MentorCardResponse cardResponse(Long id, String firstName) {
        return new MentorCardResponse(id, null, firstName, "Фамилия", null,
                null, null, null, null, null, null, null, null, List.of());
    }
}
