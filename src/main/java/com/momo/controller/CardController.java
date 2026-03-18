package com.momo.controller;

import com.momo.dto.CardCreateRequest;
import com.momo.dto.CardStatusUpdateRequest;
import com.momo.model.Account;
import com.momo.model.AccountType;
import com.momo.model.Card;
import com.momo.model.CardType;
import com.momo.store.JdbcStore;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/{id}/cards")
public class CardController {
    private static final BigDecimal CREDIT_CARD_LIMIT = BigDecimal.valueOf(10_000);

    private final JdbcStore store;

    public CardController(JdbcStore store) {
        this.store = store;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Card create(@PathVariable long id, @Valid @RequestBody CardCreateRequest request) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        if (store.getCardByAccount(id) != null) {
            throw new IllegalStateException("Account already has a card");
        }
        if (account.accountType() != AccountType.CHECKING && account.accountType() != AccountType.CREDIT) {
            throw new IllegalStateException("Cards can only be assigned to CHECKING or CREDIT accounts");
        }
        if (!isSupportedCardType(account.accountType(), request.type())) {
            throw new IllegalStateException("Card type does not match the account type");
        }

        BigDecimal cardLimit = request.type() == CardType.DEBIT ? account.balance() : CREDIT_CARD_LIMIT;
        return store.saveCard(account.id(), request.type(), cardLimit, request.status());
    }

    private boolean isSupportedCardType(AccountType accountType, CardType cardType) {
        return (accountType == AccountType.CHECKING && cardType == CardType.DEBIT)
                || (accountType == AccountType.CREDIT && cardType == CardType.CREDIT);
    }

    @GetMapping("/status")
    public Map<String, String> getStatus(@PathVariable long id) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        Card card = store.getCardByAccount(id);
        if (card == null) {
            throw new IllegalArgumentException("Card not found");
        }
        return Map.of("status", card.status().name());
    }

    @GetMapping("/limit")
    public Map<String, BigDecimal> getLimit(@PathVariable long id) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        Card card = store.getCardByAccount(id);
        if (card == null) {
            throw new IllegalArgumentException("Card not found");
        }
        return Map.of("cardLimit", card.cardLimit());
    }

    @PatchMapping("/status")
    public Card updateStatus(@PathVariable long id, @Valid @RequestBody CardStatusUpdateRequest request) {
        Account account = store.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        Card updatedCard = store.updateCardStatus(id, request.status());
        if (updatedCard == null) {
            throw new IllegalArgumentException("Card not found");
        }
        return updatedCard;
    }
}
