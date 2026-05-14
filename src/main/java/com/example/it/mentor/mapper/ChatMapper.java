package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.chat.AttachmentInfo;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.ChatMessage;
import com.example.it.mentor.entity.StoredFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "mentoringRequestId", source = "mentoringRequest.id")
    @Mapping(target = "mentoringRequestStatus", source = "mentoringRequest.status")
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "unreadCount", constant = "0")
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "mentorName", ignore = true)
    ChatResponse toResponse(Chat chat);

    @Mapping(target = "chatId", source = "chat.id")
    ChatMessageResponse toMessageResponse(ChatMessage message);

    @Mapping(target = "fileId", source = "id")
    AttachmentInfo toAttachmentInfo(StoredFile file);
}
