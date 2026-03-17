package com.momo.controller;

import com.momo.dto.TransactionCreateRequest;
import com.momo.model.Account;
import com.momo.model.Transaction;
import com.momo.model.TransactionType;
import com.momo.store.JdbcStore;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
@RequestMapping
public class TransactionController {
    private final JdbcStore store;

    public TransactionController(JdbcStore store) {
        this.store = store;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody TransactionCreateRequest request) {
        if (request.transactionType() == TransactionType.DEPOSIT) {
            return deposit(request);
        }
        if (request.transactionType() == TransactionType.WITHDRAWAL) {
            return withdrawal(request);
        }
        return transfer(request);
    }

    @GetMapping("/accounts/{id}/transactions")
    public List<Transaction> listByAccount(@PathVariable long id) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        return store.getTransactionsByAccount(id);
    }

    private Transaction deposit(TransactionCreateRequest request) {
        Account recipient = store.getAccount(request.recipientAccountId());
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient account not found");
        }
        BigDecimal newBalance = recipient.balance().add(request.amount());
        store.updateAccountBalance(recipient.id(), newBalance);
        return store.saveTransaction(null, recipient.id(), request.amount(), request.transactionType(), request.note());
    }

    private Transaction withdrawal(TransactionCreateRequest request) {
        Account sender = store.getAccount(request.senderAccountId());
        if (sender == null) {
            throw new IllegalArgumentException("Sender account not found");
        }
        if (sender.balance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        BigDecimal newBalance = sender.balance().subtract(request.amount());
        store.updateAccountBalance(sender.id(), newBalance);
        return store.saveTransaction(sender.id(), null, request.amount(), request.transactionType(), request.note());
    }

    private Transaction transfer(TransactionCreateRequest request) {
        Account sender = store.getAccount(request.senderAccountId());
        Account recipient = store.getAccount(request.recipientAccountId());
        if (sender == null || recipient == null) {
            throw new IllegalArgumentException("Sender or recipient account not found");
        }
        if (sender.balance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        store.updateAccountBalance(sender.id(), sender.balance().subtract(request.amount()));
        store.updateAccountBalance(recipient.id(), recipient.balance().add(request.amount()));
        return store.saveTransaction(sender.id(), recipient.id(), request.amount(), request.transactionType(), request.note());
    }
}
