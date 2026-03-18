package com.momo.controller;

import com.momo.health.ApplicationState;
import com.momo.store.JdbcStore;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HealthController {
    private final JdbcStore store;
    private final ApplicationState applicationState;

    public HealthController(JdbcStore store, ApplicationState applicationState) {
        this.store = store;
        this.applicationState = applicationState;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean databaseUp = store.isDatabaseAvailable();
        boolean ready = applicationState.isReady() && !applicationState.isShuttingDown() && databaseUp;
        String status = ready ? "UP" : "DOWN";

        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", status,
                        "database", databaseUp ? "UP" : "DOWN",
                        "ready", ready,
                        "shuttingDown", applicationState.isShuttingDown()
                ));
    }
}
