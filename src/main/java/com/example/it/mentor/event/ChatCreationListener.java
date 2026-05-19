package com.example.it.mentor.event;

import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.event.request.MentoringRequestAcceptedEvent;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatCreationListener {

    private final ChatService chatService;
    private final MentoringRequestRepository mentoringRequestRepository;

    // BEFORE_COMMIT: chat creation participates in the same transaction —
    // if it fails (e.g. duplicate), the whole acceptRequest rolls back.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onCreate(MentoringRequestAcceptedEvent event) {
        MentoringRequest request = mentoringRequestRepository.getReferenceById(event.requestId());
        chatService.createForRequest(request);
    }
}
