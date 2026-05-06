package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.review.CreateReviewRequest;
import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.Review;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.ReviewMapper;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.ReviewRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

    @InjectMocks private ReviewService service;

    @Mock private ReviewRepository reviewRepository;
    @Mock private MentoringRequestRepository mentoringRequestRepository;
    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private UserService userService;
    @Mock private ReviewMapper mapper;

    private User studentUser;
    private User mentorUser;
    private StudentProfile studentProfile;
    private MentorProfile mentorProfile;
    private MentoringRequest completedRequest;
    private Review review;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(studentUser, "id", 1L);

        mentorUser = User.builder().email("mentor@test.com").build();
        ReflectionTestUtils.setField(mentorUser, "id", 2L);

        studentProfile = StudentProfile.builder().user(studentUser).build();
        ReflectionTestUtils.setField(studentProfile, "id", 10L);

        mentorProfile = MentorProfile.builder().user(mentorUser).build();
        ReflectionTestUtils.setField(mentorProfile, "id", 20L);

        completedRequest = MentoringRequest.builder()
                .studentProfile(studentProfile)
                .mentorProfile(mentorProfile)
                .build();
        ReflectionTestUtils.setField(completedRequest, "id", 100L);
        completedRequest.setStatus(MentoringRequestStatus.COMPLETED);

        review = Review.builder()
                .mentoringRequest(completedRequest)
                .reviewer(studentUser)
                .mentorUserId(2L)
                .rating(5)
                .comment("Отличный ментор!")
                .build();
        ReflectionTestUtils.setField(review, "id", 300L);

        reviewResponse = new ReviewResponse(300L, 100L, 1L, 2L, 5, "Отличный ментор!", OffsetDateTime.now());
    }

    // ── createReview ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createReview")
    class CreateReview {

        @Test
        @DisplayName("happyPath — студент оставляет отзыв на завершённую заявку")
        void createReview_happyPath_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(mentoringRequestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(completedRequest));
            when(reviewRepository.existsByMentoringRequestId(100L)).thenReturn(false);
            when(reviewRepository.save(any())).thenReturn(review);
            when(mapper.toResponse(review)).thenReturn(reviewResponse);

            CreateReviewRequest dto = new CreateReviewRequest(100L, 5, "Отличный ментор!");
            ReviewResponse result = service.createReview(dto);

            assertThat(result.rating()).isEqualTo(5);
            assertThat(result.comment()).isEqualTo("Отличный ментор!");
        }

        @Test
        @DisplayName("happyPath — сохраняет правильные поля в entity")
        void createReview_happyPath_shouldSaveCorrectEntity() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(mentoringRequestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(completedRequest));
            when(reviewRepository.existsByMentoringRequestId(100L)).thenReturn(false);
            when(reviewRepository.save(any())).thenReturn(review);
            when(mapper.toResponse(review)).thenReturn(reviewResponse);

            CreateReviewRequest dto = new CreateReviewRequest(100L, 4, null);
            service.createReview(dto);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            Review saved = captor.getValue();
            assertThat(saved.getRating()).isEqualTo(4);
            assertThat(saved.getMentorUserId()).isEqualTo(2L);
            assertThat(saved.getReviewer()).isSameAs(studentUser);
            assertThat(saved.getMentoringRequest()).isSameAs(completedRequest);
        }

        @Test
        @DisplayName("requestNotFound — заявка не найдена → NotFoundException")
        void createReview_requestNotFound_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(mentoringRequestRepository.findWithProfilesById(999L)).thenReturn(Optional.empty());

            CreateReviewRequest dto = new CreateReviewRequest(999L, 5, null);

            assertThatThrownBy(() -> service.createReview(dto))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("requestNotCompleted — заявка не завершена → BusinessRuleViolationException")
        void createReview_requestNotCompleted_shouldThrowBusinessRuleViolation() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            completedRequest.setStatus(MentoringRequestStatus.ACCEPTED);
            when(mentoringRequestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(completedRequest));

            CreateReviewRequest dto = new CreateReviewRequest(100L, 5, null);

            assertThatThrownBy(() -> service.createReview(dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("notStudentOfRequest — посторонний пользователь → ForbiddenException")
        void createReview_notStudentOfRequest_shouldThrowForbidden() {
            User outsider = User.builder().email("other@test.com").build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(mentoringRequestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(completedRequest));

            CreateReviewRequest dto = new CreateReviewRequest(100L, 5, null);

            assertThatThrownBy(() -> service.createReview(dto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("mentorTriesToReview — ментор пытается оставить отзыв → ForbiddenException")
        void createReview_mentorTriesToReview_shouldThrowForbidden() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(mentoringRequestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(completedRequest));

            CreateReviewRequest dto = new CreateReviewRequest(100L, 5, null);

            assertThatThrownBy(() -> service.createReview(dto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("alreadyExists — отзыв уже существует → ConflictException")
        void createReview_alreadyExists_shouldThrowConflict() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(mentoringRequestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(completedRequest));
            when(reviewRepository.existsByMentoringRequestId(100L)).thenReturn(true);

            CreateReviewRequest dto = new CreateReviewRequest(100L, 5, null);

            assertThatThrownBy(() -> service.createReview(dto))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ── getByMentoringRequest ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getByMentoringRequest")
    class GetByMentoringRequest {

        @Test
        @DisplayName("student — студент получает свой отзыв")
        void getByMentoringRequest_student_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(reviewRepository.findByMentoringRequestId(100L)).thenReturn(Optional.of(review));
            when(mapper.toResponse(review)).thenReturn(reviewResponse);

            ReviewResponse result = service.getByMentoringRequest(100L);

            assertThat(result).isEqualTo(reviewResponse);
        }

        @Test
        @DisplayName("mentor — ментор видит отзыв о себе")
        void getByMentoringRequest_mentor_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(reviewRepository.findByMentoringRequestId(100L)).thenReturn(Optional.of(review));
            when(mapper.toResponse(review)).thenReturn(reviewResponse);

            ReviewResponse result = service.getByMentoringRequest(100L);

            assertThat(result).isEqualTo(reviewResponse);
        }

        @Test
        @DisplayName("outsider — посторонний → ForbiddenException")
        void getByMentoringRequest_outsider_shouldThrowForbidden() {
            User outsider = User.builder().email("other@test.com").build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(reviewRepository.findByMentoringRequestId(100L)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> service.getByMentoringRequest(100L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("notFound — отзыв не существует → NotFoundException")
        void getByMentoringRequest_notFound_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(reviewRepository.findByMentoringRequestId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByMentoringRequest(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── getMentorReviews ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMentorReviews")
    class GetMentorReviews {

        @Test
        @DisplayName("happyPath — возвращает пагинированный список отзывов")
        void getMentorReviews_happyPath_shouldReturnPage() {
            when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(mentorProfile));
            Page<Review> page = new PageImpl<>(List.of(review));
            when(reviewRepository.findByMentorUserId(eq(2L), any())).thenReturn(page);
            when(mapper.toResponse(review)).thenReturn(reviewResponse);

            PagedResponse<ReviewResponse> result = service.getMentorReviews(20L, PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("emptyList — нет отзывов → пустая страница")
        void getMentorReviews_emptyList_shouldReturnEmptyPage() {
            when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(mentorProfile));
            Page<Review> emptyPage = new PageImpl<>(List.of());
            when(reviewRepository.findByMentorUserId(eq(2L), any())).thenReturn(emptyPage);

            PagedResponse<ReviewResponse> result = service.getMentorReviews(20L, PageRequest.of(0, 20));

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("mentorNotFound — профиль не найден → NotFoundException")
        void getMentorReviews_mentorNotFound_shouldThrowNotFound() {
            when(mentorProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMentorReviews(999L, PageRequest.of(0, 20)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── deleteReview ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteReview")
    class DeleteReview {

        @Test
        @DisplayName("owner — автор удаляет свой отзыв")
        void deleteReview_owner_shouldDelete() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(reviewRepository.findById(300L)).thenReturn(Optional.of(review));

            service.deleteReview(300L);

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("notOwner — не автор → ForbiddenException")
        void deleteReview_notOwner_shouldThrowForbidden() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(reviewRepository.findById(300L)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> service.deleteReview(300L))
                    .isInstanceOf(ForbiddenException.class);

            verify(reviewRepository, never()).delete(any());
        }

        @Test
        @DisplayName("notFound — отзыв не существует → NotFoundException")
        void deleteReview_notFound_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteReview(999L))
                    .isInstanceOf(NotFoundException.class);

            verify(reviewRepository, never()).delete(any());
        }
    }
}
