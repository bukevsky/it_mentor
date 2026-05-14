package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.chat.ChatMessageResponse;
import com.example.it.mentor.dto.chat.ChatResponse;
import com.example.it.mentor.dto.chat.SendMessageRequest;
import com.example.it.mentor.entity.Chat;
import com.example.it.mentor.entity.ChatMessage;
import com.example.it.mentor.entity.ChatReadState;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.ChatMapper;
import com.example.it.mentor.repository.ChatMessageRepository;
import com.example.it.mentor.repository.ChatReadStateRepository;
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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatReadStateRepository readStateRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;
    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final ChatMapper mapper;
    private final ChatSseService sseService;

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
        log.info("Чат создан: chatId={}, requestId={}, studentUserId={}, mentorUserId={}",
                chat.getId(), request.getId(), studentUserId, mentorUserId);
    }

    public ChatResponse getById(Long chatId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findWithMentoringRequestById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        return enrichSingle(chat, currentUser.getId());
    }

    public ChatResponse getByRequestId(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findByMentoringRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Чат для заявки не найден: " + requestId));
        checkParticipant(chat, currentUser.getId());
        return enrichSingle(chat, currentUser.getId());
    }

    public PagedResponse<ChatResponse> getMyChats(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Long userId = currentUser.getId();

        Sort sort = Sort.by(Sort.Order.desc("lastMessageAt").nullsLast())
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Chat> page = chatRepository.findAllByUserId(userId, sortedPageable);

        Map<Long, Long> unreadMap = readStateRepository.countUnreadPerChat(userId);

        List<Long> studentUserIds = page.getContent().stream()
                .map(Chat::getStudentUserId).distinct().collect(Collectors.toList());
        List<Long> mentorUserIds = page.getContent().stream()
                .map(Chat::getMentorUserId).distinct().collect(Collectors.toList());

        Map<Long, String> studentNames = buildNameMap(
                studentProfileRepository.findAllByUserIdIn(studentUserIds),
                p -> p.getUser() != null ? p.getUser().getId() : null,
                p -> p.getFirstName() + " " + p.getLastName()
        );
        Map<Long, String> mentorNames = buildNameMap(
                mentorProfileRepository.findAllByUserIdIn(mentorUserIds),
                p -> p.getUser() != null ? p.getUser().getId() : null,
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
        return PagedResponse.from(responsePage);
    }

    public PagedResponse<ChatMessageResponse> getMessages(Long chatId, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        Page<ChatMessage> page = messageRepository
                .findByChatIdAndDeletedFalseOrderByCreatedAtDesc(chatId, pageable);
        return PagedResponse.from(page.map(mapper::toMessageResponse));
    }

    public List<ChatMessageResponse> getMessagesCursor(Long chatId, Long beforeMessageId, int limit) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        Pageable pageable = PageRequest.of(0, limit);
        return messageRepository
                .findByChatIdAndDeletedFalseAndIdLessThanOrderByIdDesc(chatId, beforeMessageId, pageable)
                .getContent()
                .stream()
                .map(mapper::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long chatId, SendMessageRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());

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
                .body(body)
                .attachment(attachment)
                .build();

        message = messageRepository.save(message);

        chat.setLastMessageAt(message.getCreatedAt());
        chat.setLastSenderUserId(currentUser.getId());
        chatRepository.save(chat);

        log.debug("Сообщение отправлено: chatId={}, senderId={}, hasAttachment={}",
                chatId, currentUser.getId(), attachment != null);

        ChatMessageResponse response = mapper.toMessageResponse(message);
        sseService.pushMessageCreated(chat.getStudentUserId(), chat.getMentorUserId(), response);
        return response;
    }

    @Transactional
    public void markAsRead(Long chatId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());

        ChatReadState state = readStateRepository
                .findByChatIdAndUserId(chatId, currentUser.getId())
                .orElseGet(() -> ChatReadState.builder()
                        .chatId(chatId)
                        .userId(currentUser.getId())
                        .lastReadAt(OffsetDateTime.now())
                        .build());
        state.setLastReadAt(OffsetDateTime.now());
        readStateRepository.save(state);

        sseService.pushReadEvent(chat.getStudentUserId(), chat.getMentorUserId(), chatId);
        log.debug("Чат отмечен как прочитанный: chatId={}, userId={}", chatId, currentUser.getId());
    }

    private ChatResponse enrichSingle(Chat chat, Long currentUserId) {
        ChatMessageResponse lastMsg = messageRepository
                .findFirstByChatIdAndDeletedFalseOrderByCreatedAtDesc(chat.getId())
                .map(mapper::toMessageResponse)
                .orElse(null);

        int unread = (int) readStateRepository.countUnreadForChat(chat.getId(), currentUserId);

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
