package com.momo.store;

import com.momo.model.Account;
import com.momo.model.AccountHolder;
import com.momo.model.AccountType;
import com.momo.model.Transaction;
import com.momo.model.TransactionType;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryStore {
    private final Map<Long, AccountHolder> holders = new ConcurrentHashMap<>();
    private final Map<Long, Account> accounts = new ConcurrentHashMap<>();
    private final Map<Long, Transaction> transactions = new ConcurrentHashMap<>();
    private final SecureRandom idGenerator = new SecureRandom();

    public AccountHolder saveHolder(String fullName, String email) {
        long id = nextId(holders);
        AccountHolder holder = new AccountHolder(id, fullName, email);
        holders.put(id, holder);
        return holder;
    }

    public AccountHolder getHolder(long id) {
        return holders.get(id);
    }

    public Account saveAccount(long holderId, AccountType type, BigDecimal balance) {
        long id = nextId(accounts);
        Account account = new Account(id, holderId, type, balance);
        accounts.put(id, account);
        return account;
    }

    public Account getAccount(long id) {
        return accounts.get(id);
    }

    public Account updateAccountBalance(long id, BigDecimal newBalance) {
        Account account = accounts.get(id);
        if (account == null) {
            return null;
        }
        Account updated = new Account(account.id(), account.holderId(), account.type(), newBalance);
        accounts.put(id, updated);
        return updated;
    }

    public List<Account> getAccountsByHolder(long holderId) {
        return accounts.values().stream()
                .filter(account -> account.holderId() == holderId)
                .toList();
    }

    public Transaction saveTransaction(Long senderAccountId, Long recipientAccountId, BigDecimal amount, TransactionType type, String note) {
        long id = nextId(transactions);
        Transaction transaction = new Transaction(id, senderAccountId, recipientAccountId, amount, type, note);
        transactions.put(id, transaction);
        return transaction;
    }

    public List<Transaction> getTransactionsByAccount(long accountId) {
        return transactions.values().stream()
                .filter(tx -> (tx.senderAccountId() != null && tx.senderAccountId() == accountId)
                        || (tx.recipientAccountId() != null && tx.recipientAccountId() == accountId))
                .toList();
    }

    private long nextId(Map<Long, ?> map) {
        long id;
        do {
            id = idGenerator.nextLong() & Long.MAX_VALUE;
        } while (map.containsKey(id));
        return id;
    }
}
