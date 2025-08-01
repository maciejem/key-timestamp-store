package com.task.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
public class KeyValueHistoryServiceConcurrencyTest {

    @SpringBootApplication
    static class KeyValueHistoryServiceTestApp {
        @Bean
        @Primary
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl(postgres.getJdbcUrl());
            ds.setUsername(postgres.getUsername());
            ds.setPassword(postgres.getPassword());
            ds.setDriverClassName(postgres.getDriverClassName());
            return ds;
        }
    }

    @Autowired
    private KeyValueHistoryService service;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("user")
        .withPassword("password");

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("DELETE FROM key_value_history");
    }

    @Test
    void testGetReturnsValueAtTimestamp() {
        var dto = new KeyValueHistoryDTO(null, "key-1", "value-1", 123456789L);

        service.put(dto);
        Optional<String> found = service.get("key-1", 123456789L);

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo("value-1");
    }

    @Test
    void testGetReturnsMostRecentValueBeforeTimestamp() {
        String key = "key-1";

        service.put(new KeyValueHistoryDTO(null, key, "value-1", 1000L));
        service.put(new KeyValueHistoryDTO(null, key, "value-2", 2000L));
        service.put(new KeyValueHistoryDTO(null, key, "value-3", 3000L));

        Optional<String> result = service.get(key, 2500L);

        assertThat(result).isPresent().contains("value-2");
    }

    @Test
    void testGetReturnsEmptyIfNoEarlierOrEqualTimestamp() {
        String key = "key-1";
        service.put(new KeyValueHistoryDTO(null, key, "value-1", 2000L));

        Optional<String> result = service.get(key, 1000L);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetReturnsEmptyForNonExistentKey() {
        Optional<String> result = service.get("key-1", 1000L);

        assertThat(result).isEmpty();
    }

    @Test
    void testConcurrentPutCallsShoulPass() {
        int threads = 10;
        var dto = new KeyValueHistoryDTO(null, "key", "value", 123456L);

        var results = IntStream.range(0, threads)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                try {
                    return service.put(dto);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }))
            .toList();

        CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).join();

        results.forEach(future -> {
            try {
                var result = future.join();
                assertNotNull(result);
                assertEquals(dto.key(), result.key());
                assertEquals(dto.timestamp(), result.timestamp());
            } catch (CompletionException e) {
                if (e.getCause() instanceof DataIntegrityViolationException) {
                    fail("DataIntegrityViolationException escaped retry handling");
                } else {
                    fail("Unexpected exception: " + e.getCause());
                }
            }
        });
    }
}
