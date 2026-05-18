package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.complaint.ComplaintResponse;
import com.example.it.mentor.entity.Complaint;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ComplaintMapper {

    ComplaintResponse toResponse(Complaint complaint);
}
