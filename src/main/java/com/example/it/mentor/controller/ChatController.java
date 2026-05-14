package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.dto.chat.TypingRequest;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.service.ChatService;
import com.example.it.mentor.service.ChatSseService;
import com.example.it.mentor.service.PresenceService;
import com.example.it.mentor.service.TypingService;
import com.example.it.mentor.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Validated
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Чат студента и ментора")
public class ChatController {

    private final ChatService chatService;
    private final ChatSseService sseService;
    private final UserService userService;
    private final PresenceService presenceService;
    private final TypingService typingService;

    @GetMapping
    public PagedResponse<ChatResponse> getMyChats(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return chatService.getMyChats(PageRequest.of(page, size));
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

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(chatId, request);
    }

    @PostMapping("/{chatId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long chatId) {
        chatService.markAsRead(chatId);
    }

    @PostMapping("/{chatId}/typing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendTyping(
            @PathVariable Long chatId,
            @Valid @RequestBody TypingRequest request) {
        typingService.handleTyping(chatId, request.typing());
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEvents() {
        User user = userService.getCurrentUserEntity();
        Long userId = user.getId();
        SseEmitter emitter = sseService.subscribe(userId, () -> presenceService.setOffline(userId));
        presenceService.setOnline(userId);
        return emitter;
    }
}
