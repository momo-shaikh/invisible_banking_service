package com.momo.controller;

import com.momo.dto.AccountCreateRequest;
import com.momo.model.Account;
import com.momo.model.AccountHolder;
import com.momo.model.AccountType;
import com.momo.store.JdbcStore;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final JdbcStore store;

    public AccountController(JdbcStore store) {
        this.store = store;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@Valid @RequestBody AccountCreateRequest request) {
        AccountHolder holder = store.getHolder(request.holderId());
        if (holder == null) {
            throw new IllegalArgumentException("Account holder not found");
        }
        if (request.accountType() == AccountType.CREDIT && request.balance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Credit accounts must start with a zero balance");
        }
        return store.saveAccount(request.holderId(), request.accountType(), request.balance());
    }

    @GetMapping("/{id}")
    public Account get(@PathVariable long id) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        return account;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        store.deleteAccount(id);
    }
}
