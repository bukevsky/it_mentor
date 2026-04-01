package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        return chatService.getMyChats(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{chatId}")
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
        return chatService.getMessages(chatId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable Long chatId,
            @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(chatId, request);
    }
}
