package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.ChatMessage;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.ChatMapper;
import com.example.it.mentor.repository.ChatMessageRepository;
import com.example.it.mentor.repository.ChatRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.StoredFileRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;
    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final ChatMapper mapper;

    @Transactional
    public void createForRequest(MentoringRequest request) {
        if (chatRepository.existsByMentoringRequestId(request.getId())) {
            throw new ConflictException("Чат для этой заявки уже существует");
        }
        Long studentUserId = request.getStudentProfile().getUser().getId();
        Long mentorUserId  = request.getMentorProfile().getUser().getId();
        Chat chat = Chat.builder()
                .mentoringRequest(request)
                .studentUserId(studentUserId)
                .mentorUserId(mentorUserId)
                .build();
        chatRepository.save(chat);
        log.info("Чат создан: chatId={}, requestId={}, studentUserId={}, mentorUserId={}, step={}",
                chat.getId(), request.getId(), studentUserId, mentorUserId, "chat_created");
    }

    public ChatResponse getById(Long chatId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findWithMentoringRequestById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        log.debug("Чат загружен: chatId={}, userId={}, step={}",
                chatId, currentUser.getId(), "chat_loaded");
        return enrichSingle(chat, currentUser.getId());
    }

    public ChatResponse getByRequestId(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findByMentoringRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Чат для заявки не найден: " + requestId));
        checkParticipant(chat, currentUser.getId());
        log.debug("Чат по заявке загружен: requestId={}, chatId={}, userId={}, step={}",
                requestId, chat.getId(), currentUser.getId(), "chat_loaded_by_request");
        return enrichSingle(chat, currentUser.getId());
    }

    public PagedResponse<ChatResponse> getMyChats(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Long userId = currentUser.getId();

        Sort sort = Sort.by(Sort.Order.desc("lastMessageAt").nullsLast())
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Chat> page = chatRepository.findAllByUserId(userId, sortedPageable);

        Map<Long, Long> unreadMap = messageRepository.countUnreadPerChat(userId);

        List<Long> studentUserIds = page.getContent().stream()
                .map(Chat::getStudentUserId).distinct().collect(Collectors.toList());
        List<Long> mentorUserIds = page.getContent().stream()
                .map(Chat::getMentorUserId).distinct().collect(Collectors.toList());

        Map<Long, String> studentNames = buildNameMap(
                studentProfileRepository.findAllByUserIdIn(studentUserIds),
                p -> p.getUser().getId(),
                p -> p.getFirstName() + " " + p.getLastName()
        );
        Map<Long, String> mentorNames = buildNameMap(
                mentorProfileRepository.findAllByUserIdIn(mentorUserIds),
                p -> p.getUser().getId(),
                p -> p.getFirstName() + " " + p.getLastName()
        );

        List<Long> chatIds = page.getContent().stream().map(Chat::getId).toList();
        Map<Long, ChatMessageResponse> lastMsgMap = chatIds.isEmpty() ? Map.of() :
                messageRepository.findLastMessagesByChatIds(chatIds).stream()
                        .collect(Collectors.toMap(m -> m.getChat().getId(), mapper::toMessageResponse));

        Page<ChatResponse> responsePage = page.map(chat -> {
            ChatMessageResponse lastMsg = lastMsgMap.get(chat.getId());
            int unread = unreadMap.getOrDefault(chat.getId(), 0L).intValue();
            String studentName = studentNames.get(chat.getStudentUserId());
            String mentorName  = mentorNames.get(chat.getMentorUserId());
            String requestStatus = chat.getMentoringRequest() != null
                    ? chat.getMentoringRequest().getStatus().name()
                    : null;
            return new ChatResponse(
                    chat.getId(),
                    chat.getMentoringRequest() != null ? chat.getMentoringRequest().getId() : null,
                    chat.getStudentUserId(),
                    chat.getMentorUserId(),
                    chat.getCreatedAt(),
                    lastMsg,
                    chat.getLastMessageAt(),
                    chat.getLastSenderUserId(),
                    unread,
                    studentName,
                    mentorName,
                    requestStatus
            );
        });
        log.debug("Список чатов загружен: userId={}, page={}, size={}, resultCount={}, total={}, step={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), responsePage.getNumberOfElements(),
                responsePage.getTotalElements(), "chats_loaded");
        return PagedResponse.from(responsePage);
    }

    public PagedResponse<ChatMessageResponse> getMessages(Long chatId, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        Page<ChatMessage> page = messageRepository
                .findByChatIdAndDeletedFalseOrderByCreatedAtDesc(chatId, pageable);
        log.debug("Сообщения чата загружены: chatId={}, userId={}, page={}, size={}, resultCount={}, total={}, step={}",
                chatId, currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize(),
                page.getNumberOfElements(), page.getTotalElements(), "chat_messages_loaded");
        return PagedResponse.from(page.map(mapper::toMessageResponse));
    }

    public List<ChatMessageResponse> getMessagesCursor(Long chatId, Long beforeMessageId, int limit) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessageResponse> messages = messageRepository
                .findByChatIdAndDeletedFalseAndIdLessThanOrderByIdDesc(chatId, beforeMessageId, pageable)
                .getContent()
                .stream()
                .map(mapper::toMessageResponse)
                .collect(Collectors.toList());
        log.debug("Сообщения чата по курсору загружены: chatId={}, userId={}, beforeMessageId={}, limit={}, " +
                        "resultCount={}, step={}",
                chatId, currentUser.getId(), beforeMessageId, limit, messages.size(), "chat_messages_cursor_loaded");
        return messages;
    }

    public List<ChatMessageResponse> getMessagesSince(Long chatId, Long afterMessageId, int limit) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        List<ChatMessageResponse> messages = messageRepository
                .findByChatIdAndDeletedFalseAndIdGreaterThanOrderByIdAsc(
                        chatId, afterMessageId, PageRequest.of(0, limit))
                .stream()
                .map(mapper::toMessageResponse)
                .toList();
        log.debug("Синхронизация сообщений выполнена: chatId={}, userId={}, afterMessageId={}, limit={}, " +
                        "resultCount={}, step={}",
                chatId, currentUser.getId(), afterMessageId, limit, messages.size(), "chat_messages_synced");
        return messages;
    }

    @Transactional
    public SendMessageResult sendMessage(Long chatId, UUID clientMessageId, SendMessageRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());

        ChatMessage duplicate = messageRepository
                .findBySenderUserIdAndClientMessageId(currentUser.getId(), clientMessageId)
                .orElse(null);
        if (duplicate != null) {
            if (!chatId.equals(duplicate.getChat().getId())) {
                throw new ConflictException("requestId уже использован в другом чате");
            }
            log.debug("Повторная отправка сообщения обработана идемпотентно: chatId={}, messageId={}, " +
                            "senderId={}, clientMessageId={}, step={}",
                    chatId, duplicate.getId(), currentUser.getId(), clientMessageId,
                    "chat_message_duplicate_returned");
            return new SendMessageResult(
                    mapper.toMessageResponse(duplicate),
                    chat.getStudentUserId(),
                    chat.getMentorUserId(),
                    true
            );
        }

        if ((dto.body() == null || dto.body().isBlank()) && dto.attachmentFileId() == null) {
            throw new BusinessRuleViolationException("Сообщение не может быть пустым");
        }

        StoredFile attachment = null;
        if (dto.attachmentFileId() != null) {
            fileStorage.requireOwned(dto.attachmentFileId(), currentUser.getId());
            attachment = storedFileRepository.findById(dto.attachmentFileId())
                    .orElseThrow(() -> new NotFoundException("Файл не найден"));
        }

        String body = (dto.body() != null && !dto.body().isBlank()) ? dto.body() : null;

        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .senderUserId(currentUser.getId())
                .clientMessageId(clientMessageId)
                .body(body)
                .attachment(attachment)
                .build();

        message = messageRepository.save(message);

        chat.setLastMessageAt(message.getCreatedAt());
        chat.setLastSenderUserId(currentUser.getId());
        chatRepository.save(chat);

        log.info("Сообщение отправлено: chatId={}, messageId={}, senderId={}, hasAttachment={}, attachmentFileId={}, step={}",
                chatId, message.getId(), currentUser.getId(), attachment != null, dto.attachmentFileId(),
                "chat_message_sent");

        ChatMessageResponse response = mapper.toMessageResponse(message);
        return new SendMessageResult(
                response,
                chat.getStudentUserId(),
                chat.getMentorUserId(),
                false
        );
    }

    private ChatResponse enrichSingle(Chat chat, Long currentUserId) {
        ChatMessageResponse lastMsg = messageRepository
                .findFirstByChatIdAndDeletedFalseOrderByCreatedAtDesc(chat.getId())
                .map(mapper::toMessageResponse)
                .orElse(null);

        int unread = (int) messageRepository.countUnreadForChat(chat.getId(), currentUserId);

        String studentName = studentProfileRepository.findByUserId(chat.getStudentUserId())
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse(null);
        String mentorName = mentorProfileRepository.findByUserId(chat.getMentorUserId())
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse(null);
        String requestStatus = chat.getMentoringRequest() != null
                ? chat.getMentoringRequest().getStatus().name()
                : null;

        return new ChatResponse(
                chat.getId(),
                chat.getMentoringRequest() != null ? chat.getMentoringRequest().getId() : null,
                chat.getStudentUserId(),
                chat.getMentorUserId(),
                chat.getCreatedAt(),
                lastMsg,
                chat.getLastMessageAt(),
                chat.getLastSenderUserId(),
                unread,
                studentName,
                mentorName,
                requestStatus
        );
    }

    private <T> Map<Long, String> buildNameMap(Collection<T> profiles,
                                                Function<T, Long> userId,
                                                Function<T, String> name) {
        return profiles.stream()
                .filter(p -> userId.apply(p) != null)
                .collect(Collectors.toMap(userId, name, (a, b) -> a));
    }

    private void checkParticipant(Chat chat, Long userId) {
        if (!userId.equals(chat.getStudentUserId()) && !userId.equals(chat.getMentorUserId())) {
            throw new ForbiddenException("Доступ к чату запрещён");
        }
    }
}
