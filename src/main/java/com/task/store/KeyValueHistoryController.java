package com.task.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/store")
@Validated
public class KeyValueHistoryController {

    private final KeyValueHistoryService keyValueHistoryService;

    public KeyValueHistoryController(KeyValueHistoryService keyValueHistoryService) {
        this.keyValueHistoryService = keyValueHistoryService;
    }

    @PutMapping
    public ResponseEntity<KeyValueHistoryDTO> put(@Valid @RequestBody KeyValueHistoryDTO request) {
        KeyValueHistoryDTO keyValueHistoryDTO = keyValueHistoryService.put(request);

        return ResponseEntity.ok().body(keyValueHistoryDTO);
    }

    @GetMapping
    public ResponseEntity<String> get(@RequestParam @NotNull String key, @RequestParam @NotNull Long timestamp) {
        Optional<String> value = keyValueHistoryService.get(key, timestamp);

        return value.map(v -> ResponseEntity.ok().body(v))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
