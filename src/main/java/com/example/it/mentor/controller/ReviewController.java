package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.review.CreateReviewRequest;
import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для работы с отзывами студентов о менторах.
 *
 * <p>Создание и удаление отзывов требуют аутентификации, а список отзывов ментора
 * может быть доступен публично согласно настройкам безопасности.</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Отзывы студентов о менторах")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Создаёт отзыв по завершённой заявке на менторство.
     *
     * @param request данные нового отзыва
     * @return созданный отзыв
     */
    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }

    /**
     * Возвращает отзыв, привязанный к заявке на менторство.
     *
     * @param requestId идентификатор заявки
     * @return найденный отзыв
     */
    @GetMapping("/reviews/by-request/{requestId}")
    public ReviewResponse getByRequest(@PathVariable Long requestId) {
        return reviewService.getByMentoringRequest(requestId);
    }

    /**
     * Возвращает страницу отзывов указанного ментора.
     *
     * @param id идентификатор профиля ментора
     * @param page номер страницы, начиная с {@code 0}
     * @param size размер страницы
     * @return страница отзывов по ментору
     */
    @GetMapping("/profiles/mentors/{id}/reviews")
    public PagedResponse<ReviewResponse> getMentorReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.getMentorReviews(id, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Удаляет отзыв его автором.
     *
     * @param id идентификатор отзыва
     */
    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }
}
