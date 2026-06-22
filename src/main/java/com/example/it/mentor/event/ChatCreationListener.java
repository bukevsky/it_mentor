package com.example.it.mentor.event;

import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.event.request.MentoringRequestAcceptedEvent;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCreationListener {

    private final ChatService chatService;
    private final MentoringRequestRepository mentoringRequestRepository;

    // BEFORE_COMMIT: chat creation participates in the same transaction —
    // if it fails (e.g. duplicate), the whole acceptRequest rolls back.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onCreate(MentoringRequestAcceptedEvent event) {
        log.debug("Создание чата по принятой заявке начато: requestId={}, recipientUserId={}, eventType={}, step={}",
                event.requestId(), event.recipientUserId(), event.type(), "chat_creation_listener_started");
        MentoringRequest request = mentoringRequestRepository.getReferenceById(event.requestId());
        chatService.createForRequest(request);
        log.debug("Создание чата по принятой заявке завершено: requestId={}, eventType={}, step={}",
                event.requestId(), event.type(), "chat_creation_listener_completed");
    }
}
