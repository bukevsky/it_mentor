package com.example.it.mentor.event.audit;

import com.example.it.mentor.entity.enums.DictionaryOperation;
import com.example.it.mentor.entity.enums.DictionaryType;

public record DictionaryChangedAuditEvent(
        Long adminUserId,
        DictionaryType dictionaryType,
        Long entryId,
        DictionaryOperation operation,
        String entryName) implements AuditEvent {}
