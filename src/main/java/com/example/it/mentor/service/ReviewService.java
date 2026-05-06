package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.review.CreateReviewRequest;
import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.Review;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис создания, просмотра и удаления отзывов о менторстве.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MentoringRequestRepository mentoringRequestRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;
    private final ReviewMapper mapper;

    /**
     * Создаёт отзыв по завершённой заявке.
     *
     * @param dto данные нового отзыва
     * @return созданный отзыв
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest dto) {
        User currentUser = userService.getCurrentUserEntity();

        MentoringRequest request = mentoringRequestRepository.findWithProfilesById(dto.mentoringRequestId())
                .orElseThrow(() -> new NotFoundException("Заявка не найдена: " + dto.mentoringRequestId()));

        if (request.getStatus() != MentoringRequestStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Отзыв можно оставить только по завершённой заявке");
        }

        Long studentUserId = request.getStudentProfile().getUser().getId();
        if (!currentUser.getId().equals(studentUserId)) {
            throw new ForbiddenException("Только студент данной заявки может оставить отзыв");
        }

        if (reviewRepository.existsByMentoringRequestId(dto.mentoringRequestId())) {
            throw new ConflictException("Отзыв по этой заявке уже существует");
        }

        Long mentorUserId = request.getMentorProfile().getUser().getId();

        Review review = Review.builder()
                .mentoringRequest(request)
                .reviewer(currentUser)
                .mentorUserId(mentorUserId)
                .rating(dto.rating())
                .comment(dto.comment())
                .build();

        review = reviewRepository.save(review);
        log.info("Отзыв создан: reviewId={}, requestId={}, reviewerUserId={}, mentorUserId={}, rating={}",
                review.getId(), request.getId(), currentUser.getId(), mentorUserId, dto.rating());
        return mapper.toResponse(review);
    }

    /**
     * Возвращает отзыв по идентификатору заявки.
     *
     * @param requestId идентификатор заявки
     * @return найденный отзыв
     */
    public ReviewResponse getByMentoringRequest(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();

        Review review = reviewRepository.findByMentoringRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Отзыв по заявке не найден: " + requestId));

        Long reviewerUserId = review.getReviewer().getId();
        if (!currentUser.getId().equals(reviewerUserId) && !currentUser.getId().equals(review.getMentorUserId())) {
            throw new ForbiddenException("Доступ к отзыву запрещён");
        }

        return mapper.toResponse(review);
    }

    /**
     * Возвращает страницу отзывов по профилю ментора.
     *
     * @param mentorProfileId идентификатор профиля ментора
     * @param pageable параметры пагинации
     * @return страница отзывов
     */
    public PagedResponse<ReviewResponse> getMentorReviews(Long mentorProfileId, Pageable pageable) {
        MentorProfile mentorProfile = mentorProfileRepository.findById(mentorProfileId)
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден: " + mentorProfileId));
        Long mentorUserId = mentorProfile.getUser().getId();
        Page<Review> page = reviewRepository.findByMentorUserId(mentorUserId, pageable);
        return PagedResponse.from(page.map(mapper::toResponse));
    }

    /**
     * Удаляет отзыв, если запрос выполнил его автор.
     *
     * @param reviewId идентификатор отзыва
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        User currentUser = userService.getCurrentUserEntity();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Отзыв не найден: " + reviewId));

        if (!currentUser.getId().equals(review.getReviewer().getId())) {
            throw new ForbiddenException("Удалить отзыв может только его автор");
        }

        reviewRepository.delete(review);
        log.info("Отзыв удалён: reviewId={}, reviewerUserId={}", reviewId, currentUser.getId());
    }
}
