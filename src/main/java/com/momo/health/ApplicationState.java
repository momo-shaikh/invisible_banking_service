package com.momo.health;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class ApplicationState {
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        ready.set(true);
    }

    @PreDestroy
    public void onShutdown() {
        ready.set(false);
        shuttingDown.set(true);
    }

    public boolean isReady() {
        return ready.get();
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
