package com.momo.controller;

import com.momo.dto.AccountHolderCreateRequest;
import com.momo.model.Account;
import com.momo.model.AccountHolder;
import com.momo.store.JdbcStore;
import jakarta.validation.Valid;
import java.util.List;
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
    private final JdbcStore store;

    public AccountHolderController(JdbcStore store) {
        this.store = store;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountHolder create(@Valid @RequestBody AccountHolderCreateRequest request) {
        return store.saveHolder(request.fullName(), request.email());
    }

    @GetMapping("/{id}")
    public AccountHolder get(@PathVariable long id) {
        AccountHolder holder = store.getHolder(id);
        if (holder == null) {
            throw new IllegalArgumentException("Account holder not found");
        }
        return holder;
    }

    @GetMapping("/{id}/accounts")
    public List<Account> listAccounts(@PathVariable long id) {
        AccountHolder holder = store.getHolder(id);
        if (holder == null) {
            throw new IllegalArgumentException("Account holder not found");
        }
        return store.getAccountsByHolder(id);
    }
}
