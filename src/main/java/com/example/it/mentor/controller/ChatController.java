package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.service.ChatService;
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
 * REST-контроллер для работы с чатами по заявкам на менторство.
 *
 * <p>Все операции доступны только участникам соответствующего чата.
 * Сообщения и списки чатов возвращаются постранично.</p>
 */
@Validated
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Чат студента и ментора")
public class ChatController {

    private final ChatService chatService;

    /**
     * Возвращает список чатов текущего пользователя.
     *
     * @param page номер страницы, начиная с {@code 0}
     * @param size размер страницы
     * @return страница чатов, в которых участвует текущий пользователь
     */
    @GetMapping
    public PagedResponse<ChatResponse> getMyChats(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return chatService.getMyChats(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Возвращает чат по его идентификатору.
     *
     * @param chatId идентификатор чата
     * @return данные чата
     */
    @GetMapping("/{chatId}")
    public ChatResponse getById(@PathVariable Long chatId) {
        return chatService.getById(chatId);
    }

    /**
     * Возвращает чат, созданный для конкретной заявки на менторство.
     *
     * @param requestId идентификатор заявки
     * @return данные чата
     */
    @GetMapping("/by-request/{requestId}")
    public ChatResponse getByRequestId(@PathVariable Long requestId) {
        return chatService.getByRequestId(requestId);
    }

    /**
     * Возвращает сообщения выбранного чата.
     *
     * @param chatId идентификатор чата
     * @param page номер страницы, начиная с {@code 0}
     * @param size размер страницы
     * @return страница сообщений в обратном хронологическом порядке
     */
    @GetMapping("/{chatId}/messages")
    public PagedResponse<ChatMessageResponse> getMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return chatService.getMessages(chatId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Отправляет сообщение в чат.
     *
     * <p>Сообщение может содержать текст, вложение или обе части сразу.</p>
     *
     * @param chatId идентификатор чата
     * @param request тело сообщения
     * @return сохранённое сообщение
     */
    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(chatId, request);
    }
}
