package com.momo.controller;

import com.momo.model.Account;
import com.momo.model.Statement;
import com.momo.model.Transaction;
import com.momo.store.JdbcStore;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class StatementController {
    private final JdbcStore store;

    public StatementController(JdbcStore store) {
        this.store = store;
    }

    @GetMapping("/accounts/{id}/statement")
    public Statement get(
            @PathVariable long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        List<Transaction> allTransactions = store.getTransactionsByAccount(id).stream()
                .sorted(Comparator.comparing(Transaction::createdAt))
                .toList();

        LocalDate resolvedFromDate = fromDate != null
                ? fromDate
                : allTransactions.isEmpty()
                ? LocalDate.now(ZoneOffset.UTC)
                : allTransactions.get(0).createdAt().atZone(ZoneOffset.UTC).toLocalDate();

        LocalDate resolvedToDate = toDate != null
                ? toDate
                : allTransactions.isEmpty()
                ? LocalDate.now(ZoneOffset.UTC)
                : allTransactions.get(allTransactions.size() - 1).createdAt().atZone(ZoneOffset.UTC).toLocalDate();

        if (resolvedFromDate.isAfter(resolvedToDate)) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }

        List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(transaction -> {
                    LocalDate transactionDate = transaction.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
                    return !transactionDate.isBefore(resolvedFromDate) && !transactionDate.isAfter(resolvedToDate);
                })
                .toList();

        return new Statement(
                account.id(),
                resolvedFromDate,
                resolvedToDate,
                account.balance(),
                filteredTransactions
        );
    }
}
