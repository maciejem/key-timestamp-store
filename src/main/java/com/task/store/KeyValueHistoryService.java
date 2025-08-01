
package com.task.store;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class KeyValueHistoryService {

    private final KeyValueHistoryRepository repository;

    public KeyValueHistoryService(KeyValueHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Retryable(
        retryFor = {DataIntegrityViolationException.class}
    )
    public KeyValueHistoryDTO put(KeyValueHistoryDTO keyValueHistoryDTO) {
        Optional<KeyValueHistory> existingKeyValueHistoryOptional = repository.findByKeyAndTimestamp(
            keyValueHistoryDTO.key(),
            keyValueHistoryDTO.timestamp()
        );

        KeyValueHistory keyValueHistory = existingKeyValueHistoryOptional.map(existingKeyValueHistory -> {
            existingKeyValueHistory.setValue(keyValueHistoryDTO.value());
            return existingKeyValueHistory;
        }).orElseGet(() -> new KeyValueHistory(
            keyValueHistoryDTO.key(),
            keyValueHistoryDTO.value(),
            keyValueHistoryDTO.timestamp()
        ));

        KeyValueHistory saved = repository.save(keyValueHistory);
        return new KeyValueHistoryDTO(saved);
    }

    @Transactional(readOnly = true)
    public Optional<String> get(String key, Long timestamp) {
        return repository.findTopByKeyAndTimestampLessThanEqualOrderByTimestampDesc(key, timestamp)
            .map(KeyValueHistory::getValue);
    }
}
