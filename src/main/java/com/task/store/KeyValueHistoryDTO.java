package com.task.store;

import jakarta.validation.constraints.NotNull;

public record KeyValueHistoryDTO(Long id, @NotNull String key, @NotNull String value, @NotNull Long timestamp) {
    public KeyValueHistoryDTO(KeyValueHistory keyValueHistory){
        this(keyValueHistory.getId(), keyValueHistory.getKey(), keyValueHistory.getValue(), keyValueHistory.getTimestamp()); // delegate to canonical constructor
    }
}
