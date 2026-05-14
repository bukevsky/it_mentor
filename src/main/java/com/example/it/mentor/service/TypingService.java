package com.example.it.mentor.service;

import com.example.it.mentor.dto.sse.TypingPayload;
import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypingService {

    private final ChatRepository chatRepository;
    private final ChatSseService sseService;
    private final UserService userService;

    public void handleTyping(Long chatId, boolean typing) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());

        Long targetUserId = currentUser.getId().equals(chat.getStudentUserId())
                ? chat.getMentorUserId()
                : chat.getStudentUserId();

        TypingPayload payload = new TypingPayload(chatId, currentUser.getId(), typing);
        sseService.pushTyping(targetUserId, payload);
        log.debug("Typing-событие: chatId={}, senderId={}, typing={}", chatId, currentUser.getId(), typing);
    }

    private void checkParticipant(Chat chat, Long userId) {
        if (!userId.equals(chat.getStudentUserId()) && !userId.equals(chat.getMentorUserId())) {
            throw new ForbiddenException("Доступ к чату запрещён");
        }
    }
}
