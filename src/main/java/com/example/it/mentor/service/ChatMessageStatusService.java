package com.example.it.mentor.service;

import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.ChatMessage;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.ChatMessageDeliveryStatus;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.ChatMessageRepository;
import com.example.it.mentor.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageStatusService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;

    @Transactional
    public MessageStatusChangeResult markDelivered(Long chatId, Long upToMessageId) {
        StatusContext context = validate(chatId, upToMessageId);
        OffsetDateTime now = OffsetDateTime.now();
        int changed = messageRepository.markDelivered(chatId, context.actorUserId(), upToMessageId, now);
        log.debug("Сообщения доставлены: chatId={}, actorUserId={}, upToMessageId={}, changedCount={}, step={}",
                chatId, context.actorUserId(), upToMessageId, changed, "chat_messages_delivered");
        return result(chatId, context, upToMessageId, ChatMessageDeliveryStatus.DELIVERED, now, changed);
    }

    @Transactional
    public MessageStatusChangeResult markRead(Long chatId, Long upToMessageId) {
        StatusContext context = validate(chatId, upToMessageId);
        OffsetDateTime now = OffsetDateTime.now();
        int changed = messageRepository.markRead(chatId, context.actorUserId(), upToMessageId, now);
        log.info("Сообщения прочитаны: chatId={}, actorUserId={}, upToMessageId={}, changedCount={}, step={}",
                chatId, context.actorUserId(), upToMessageId, changed, "chat_messages_read");
        return result(chatId, context, upToMessageId, ChatMessageDeliveryStatus.READ, now, changed);
    }

    private StatusContext validate(Long chatId, Long upToMessageId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());

        ChatMessage boundary = messageRepository.findById(upToMessageId)
                .orElseThrow(() -> new NotFoundException("Сообщение не найдено: " + upToMessageId));
        if (!chatId.equals(boundary.getChat().getId())) {
            throw new ForbiddenException("Сообщение принадлежит другому чату");
        }
        if (currentUser.getId().equals(boundary.getSenderUserId())) {
            throw new ForbiddenException("Нельзя подтверждать собственное сообщение");
        }
        return new StatusContext(currentUser.getId(), boundary.getSenderUserId());
    }

    private MessageStatusChangeResult result(Long chatId,
                                             StatusContext context,
                                             Long upToMessageId,
                                             ChatMessageDeliveryStatus status,
                                             OffsetDateTime changedAt,
                                             int changedCount) {
        return new MessageStatusChangeResult(
                chatId,
                context.actorUserId(),
                context.targetUserId(),
                upToMessageId,
                status,
                changedAt,
                changedCount
        );
    }

    private void checkParticipant(Chat chat, Long userId) {
        if (!userId.equals(chat.getStudentUserId()) && !userId.equals(chat.getMentorUserId())) {
            throw new ForbiddenException("Доступ к чату запрещён");
        }
    }

    private record StatusContext(Long actorUserId, Long targetUserId) {
    }
}
