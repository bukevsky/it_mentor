package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestClarifyRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestRejectRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.service.MentoringRequestService;
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
 * REST-контроллер для управления заявками на менторство.
 *
 * <p>Контроллер покрывает полный жизненный цикл заявки: создание, просмотр,
 * принятие в работу, запрос уточнений, принятие, отклонение, отмену и завершение.</p>
 */
@Validated
@RestController
@RequestMapping("/mentoring/requests")
@RequiredArgsConstructor
@Tag(name = "Mentoring Requests", description = "Система заявок на менторство")
public class MentoringController {

    private final MentoringRequestService mentoringRequestService;

    /**
     * Создаёт новую заявку на менторство.
     *
     * @param request данные новой заявки
     * @return созданная заявка
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MentoringRequestResponse createRequest(@Valid @RequestBody MentoringRequestCreateRequest request) {
        return mentoringRequestService.createRequest(request);
    }

    /**
     * Возвращает страницу заявок текущего пользователя.
     *
     * @param status необязательный фильтр по статусу
     * @param page номер страницы, начиная с {@code 0}
     * @param size размер страницы
     * @return страница заявок
     */
    @GetMapping
    public PagedResponse<MentoringRequestResponse> getRequests(
            @RequestParam(required = false) MentoringRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mentoringRequestService.getRequests(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Возвращает заявку по идентификатору.
     *
     * @param id идентификатор заявки
     * @return найденная заявка
     */
    @GetMapping("/{id}")
    public MentoringRequestResponse getById(@PathVariable Long id) {
        return mentoringRequestService.getById(id);
    }

    /**
     * Переводит заявку в статус просмотра адресатом.
     *
     * @param id идентификатор заявки
     * @return обновлённая заявка
     */
    @PutMapping("/{id}/view")
    public MentoringRequestResponse markAsReviewing(@PathVariable Long id) {
        return mentoringRequestService.markAsReviewing(id);
    }

    /**
     * Запрашивает у инициатора дополнительные сведения по заявке.
     *
     * @param id идентификатор заявки
     * @param request данные с пояснением для уточнения
     * @return обновлённая заявка
     */
    @PutMapping("/{id}/needs-clarification")
    public MentoringRequestResponse requestClarification(
            @PathVariable Long id,
            @Valid @RequestBody MentoringRequestClarifyRequest request) {
        return mentoringRequestService.requestClarification(id, request);
    }

    /**
     * Принимает заявку на менторство.
     *
     * @param id идентификатор заявки
     * @return обновлённая заявка
     */
    @PutMapping("/{id}/accept")
    public MentoringRequestResponse acceptRequest(@PathVariable Long id) {
        return mentoringRequestService.acceptRequest(id);
    }

    /**
     * Отклоняет заявку на менторство.
     *
     * @param id идентификатор заявки
     * @param request причина отклонения
     * @return обновлённая заявка
     */
    @PutMapping("/{id}/reject")
    public MentoringRequestResponse rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody MentoringRequestRejectRequest request) {
        return mentoringRequestService.rejectRequest(id, request);
    }

    /**
     * Отменяет заявку её инициатором до финальной обработки.
     *
     * @param id идентификатор заявки
     * @return обновлённая заявка
     */
    @PutMapping("/{id}/cancel")
    public MentoringRequestResponse cancelRequest(@PathVariable Long id) {
        return mentoringRequestService.cancelRequest(id);
    }

    /**
     * Завершает принятую заявку после окончания взаимодействия.
     *
     * @param id идентификатор заявки
     * @return обновлённая заявка
     */
    @PutMapping("/{id}/complete")
    public MentoringRequestResponse completeRequest(@PathVariable Long id) {
        return mentoringRequestService.completeRequest(id);
    }
}
