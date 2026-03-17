package com.momo.controller;

import com.momo.dto.CardCreateRequest;
import com.momo.model.Account;
import com.momo.model.AccountType;
import com.momo.model.Card;
import com.momo.model.CardType;
import com.momo.store.JdbcStore;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CardController {
    private static final BigDecimal CREDIT_CARD_LIMIT = BigDecimal.valueOf(10_000);

    private final JdbcStore store;

    public CardController(JdbcStore store) {
        this.store = store;
    }

    @PostMapping("/accounts/{id}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public Card create(@PathVariable long id, @Valid @RequestBody CardCreateRequest request) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        if (account.accountType() != AccountType.CHECKING && account.accountType() != AccountType.CREDIT) {
            throw new IllegalStateException("Cards can only be assigned to CHECKING or CREDIT accounts");
        }

        BigDecimal cardLimit = request.type() == CardType.DEBIT ? account.balance() : CREDIT_CARD_LIMIT;
        return store.saveCard(account.id(), request.type(), cardLimit, request.status());
    }
}
