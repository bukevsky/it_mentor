package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.review.ReviewResponse;
import com.example.it.mentor.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "mentoringRequestId", source = "mentoringRequest.id")
    @Mapping(target = "reviewerUserId", source = "reviewer.id")
    ReviewResponse toResponse(Review review);
}
