package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Чат студента и ментора")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public PagedResponse<ChatResponse> getMyChats(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return chatService.getMyChats(PageRequest.of(page, size));
    }

    @GetMapping("/{chatId:\\d+}")
    public ChatResponse getById(@PathVariable Long chatId) {
        return chatService.getById(chatId);
    }

    @GetMapping("/by-request/{requestId}")
    public ChatResponse getByRequestId(@PathVariable Long requestId) {
        return chatService.getByRequestId(requestId);
    }

    @GetMapping("/{chatId}/messages")
    public PagedResponse<ChatMessageResponse> getMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return chatService.getMessages(chatId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{chatId}/messages/cursor")
    public List<ChatMessageResponse> getMessagesCursor(
            @PathVariable Long chatId,
            @RequestParam Long beforeMessageId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return chatService.getMessagesCursor(chatId, beforeMessageId, limit);
    }

    @GetMapping("/{chatId}/messages/sync")
    public List<ChatMessageResponse> syncMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") @Min(0) Long afterMessageId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return chatService.getMessagesSince(chatId, afterMessageId, limit);
    }
}
