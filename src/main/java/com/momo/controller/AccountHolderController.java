package com.momo.controller;

import com.momo.dto.AccountHolderCreateRequest;
import com.momo.model.AccountHolder;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holders")
public class AccountHolderController {
    private final Map<Long, AccountHolder> holders = new ConcurrentHashMap<>();
    private final SecureRandom idGenerator = new SecureRandom();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountHolder create(@Valid @RequestBody AccountHolderCreateRequest request) {
        long id = nextId();
        AccountHolder holder = new AccountHolder(id, request.fullName(), request.email());
        holders.put(id, holder);
        return holder;
    }

    @GetMapping("/{id}")
    public AccountHolder get(@PathVariable long id) {
        AccountHolder holder = holders.get(id);
        if (holder == null) {
            throw new IllegalArgumentException("Account holder not found");
        }
        return holder;
    }

    private long nextId() {
        long id;
        do {
            id = idGenerator.nextLong() & Long.MAX_VALUE;
        } while (id == 0 || holders.containsKey(id));
        return id;
    }
}
