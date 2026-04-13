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
import com.example.it.mentor.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис управления чатами и сообщениями по заявкам на менторство.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;
    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final ChatMapper mapper;

    /**
     * Создаёт чат для принятой заявки на менторство.
     *
     * @param request заявка, для которой создаётся чат
     */
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
        log.info("Чат создан: chatId={}, requestId={}, studentUserId={}, mentorUserId={}", chat.getId(), request.getId(), studentUserId, mentorUserId);
    }

    /**
     * Возвращает чат по идентификатору после проверки участия текущего пользователя.
     *
     * @param chatId идентификатор чата
     * @return данные чата
     */
    public ChatResponse getById(Long chatId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findWithMentoringRequestById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        return mapper.toResponse(chat);
    }

    /**
     * Возвращает чат по идентификатору заявки на менторство.
     *
     * @param requestId идентификатор заявки
     * @return данные чата
     */
    public ChatResponse getByRequestId(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findByMentoringRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Чат для заявки не найден: " + requestId));
        checkParticipant(chat, currentUser.getId());
        return mapper.toResponse(chat);
    }

    /**
     * Возвращает страницу чатов текущего пользователя.
     *
     * @param pageable параметры пагинации
     * @return страница чатов
     */
    public PagedResponse<ChatResponse> getMyChats(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Page<Chat> page = chatRepository.findAllByUserId(currentUser.getId(), pageable);
        return PagedResponse.from(page.map(mapper::toResponse));
    }

    /**
     * Возвращает страницу сообщений указанного чата.
     *
     * @param chatId идентификатор чата
     * @param pageable параметры пагинации
     * @return страница сообщений
     */
    public PagedResponse<ChatMessageResponse> getMessages(Long chatId, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        checkParticipant(chat, currentUser.getId());
        Page<ChatMessage> page = messageRepository.findByChatIdAndDeletedFalseOrderByCreatedAtDesc(chatId, pageable);
        return PagedResponse.from(page.map(mapper::toMessageResponse));
    }

    /**
     * Сохраняет новое сообщение в чате.
     *
     * @param chatId идентификатор чата
     * @param dto данные сообщения
     * @return сохранённое сообщение
     */
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
        log.debug("Сообщение отправлено: chatId={}, senderId={}, hasAttachment={}", chatId, currentUser.getId(), attachment != null);
        return mapper.toMessageResponse(message);
    }

    /**
     * Проверяет, что указанный пользователь участвует в чате.
     *
     * @param chat чат для проверки
     * @param userId идентификатор пользователя
     */
    private void checkParticipant(Chat chat, Long userId) {
        if (!userId.equals(chat.getStudentUserId()) && !userId.equals(chat.getMentorUserId())) {
            throw new ForbiddenException("Доступ к чату запрещён");
        }
    }
}
