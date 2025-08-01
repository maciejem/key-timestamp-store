
package com.task.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KeyValueHistoryRepository extends JpaRepository<KeyValueHistory, Long> {
    Optional<KeyValueHistory> findTopByKeyAndTimestampLessThanEqualOrderByTimestampDesc(String key, Long timestamp);
    Optional<KeyValueHistory> findByKeyAndTimestamp(String key, Long timestamp);
}
