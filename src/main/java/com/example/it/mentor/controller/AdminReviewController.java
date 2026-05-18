package com.example.it.mentor.controller;

import com.example.it.mentor.dto.review.ModerateReviewRequest;
import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin: Reviews", description = "Модерация отзывов")
public class AdminReviewController {

    private final ReviewService reviewService;

    @PutMapping("/{id}/moderate")
    public ReviewResponse moderate(@PathVariable Long id,
                                   @Valid @RequestBody ModerateReviewRequest request) {
        return reviewService.moderate(id, request);
    }
}
