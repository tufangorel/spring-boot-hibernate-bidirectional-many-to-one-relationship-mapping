package com.company.customerinfo.service;

import com.company.customerinfo.model.IdempotencyRecord;
import com.company.customerinfo.repository.IdempotencyRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Integer> findResourceId(String idempotencyKey, String entityType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return idempotencyRecordRepository.findByIdempotencyKeyAndEntityType(idempotencyKey, entityType)
                .map(IdempotencyRecord::getResourceId);
    }

    @Transactional
    public void saveRecord(String idempotencyKey, String entityType, Integer resourceId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, entityType, resourceId);
            idempotencyRecordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Idempotency record already exists for key={} entityType={}", idempotencyKey, entityType, ex);
        }
    }
}
