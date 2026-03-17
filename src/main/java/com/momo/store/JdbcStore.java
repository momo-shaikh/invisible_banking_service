package com.momo.store;

import com.momo.model.Account;
import com.momo.model.AccountHolder;
import com.momo.model.AccountType;
import com.momo.model.Card;
import com.momo.model.CardStatus;
import com.momo.model.CardType;
import com.momo.model.Transaction;
import com.momo.model.TransactionType;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class JdbcStore {
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom idGenerator = new SecureRandom();

    public JdbcStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AccountHolder saveHolder(String fullName, String email) {
        long id = nextId("account_holders");
        jdbcTemplate.update(
                "INSERT INTO account_holders (id, full_name, email) VALUES (?, ?, ?)",
                id,
                fullName,
                email
        );
        return new AccountHolder(id, fullName, email);
    }

    public AccountHolder getHolder(long id) {
        List<AccountHolder> results = jdbcTemplate.query(
                "SELECT id, full_name, email FROM account_holders WHERE id = ?",
                holderRowMapper(),
                id
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public Account saveAccount(long holderId, AccountType type, BigDecimal balance) {
        long id = nextId("accounts");
        jdbcTemplate.update(
                "INSERT INTO accounts (id, holder_id, type, balance) VALUES (?, ?, ?, ?)",
                id,
                holderId,
                type.name(),
                balance
        );
        return new Account(id, holderId, type, balance);
    }

    public Account getAccount(long id) {
        List<Account> results = jdbcTemplate.query(
                "SELECT id, holder_id, type, balance FROM accounts WHERE id = ?",
                accountRowMapper(),
                id
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public Account updateAccountBalance(long id, BigDecimal newBalance) {
        int updated = jdbcTemplate.update(
                "UPDATE accounts SET balance = ? WHERE id = ?",
                newBalance,
                id
        );
        if (updated == 0) {
            return null;
        }
        return getAccount(id);
    }

    public List<Account> getAccountsByHolder(long holderId) {
        return jdbcTemplate.query(
                "SELECT id, holder_id, type, balance FROM accounts WHERE holder_id = ?",
                accountRowMapper(),
                holderId
        );
    }

    public Card saveCard(long accountId, CardType type, BigDecimal cardLimit, CardStatus status) {
        long id = nextId("cards");
        jdbcTemplate.update(
                "INSERT INTO cards (id, account_id, type, card_limit, status) VALUES (?, ?, ?, ?, ?)",
                id,
                accountId,
                type.name(),
                cardLimit,
                status.name()
        );
        return new Card(id, accountId, type, cardLimit, status);
    }

    public Card getCardByAccount(long accountId) {
        List<Card> results = jdbcTemplate.query(
                "SELECT id, account_id, type, card_limit, status FROM cards WHERE account_id = ?",
                cardRowMapper(),
                accountId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public Card getCard(long cardId) {
        List<Card> results = jdbcTemplate.query(
                "SELECT id, account_id, type, card_limit, status FROM cards WHERE id = ?",
                cardRowMapper(),
                cardId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public Card updateCardStatus(long accountId, CardStatus status) {
        int updated = jdbcTemplate.update(
                "UPDATE cards SET status = ? WHERE account_id = ?",
                status.name(),
                accountId
        );
        if (updated == 0) {
            return null;
        }
        return getCardByAccount(accountId);
    }

    public Transaction saveTransaction(Long senderAccountId, Long recipientAccountId, BigDecimal amount, TransactionType type, String note) {
        long id = nextId("transactions");
        jdbcTemplate.update(
                "INSERT INTO transactions (id, sender_account_id, recipient_account_id, amount, type, note) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                senderAccountId,
                recipientAccountId,
                amount,
                type.name(),
                note
        );
        return new Transaction(id, senderAccountId, recipientAccountId, amount, type, note);
    }

    public List<Transaction> getTransactionsByAccount(long accountId) {
        return jdbcTemplate.query(
                "SELECT id, sender_account_id, recipient_account_id, amount, type, note FROM transactions " +
                        "WHERE sender_account_id = ? OR recipient_account_id = ?",
                transactionRowMapper(),
                accountId,
                accountId
        );
    }

    private long nextId(String table) {
        long id;
        do {
            id = idGenerator.nextLong() & Long.MAX_VALUE;
        } while (existsId(table, id));
        return id;
    }

    private boolean existsId(String table, long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    private RowMapper<AccountHolder> holderRowMapper() {
        return (rs, rowNum) -> new AccountHolder(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email")
        );
    }

    private RowMapper<Account> accountRowMapper() {
        return (rs, rowNum) -> new Account(
                rs.getLong("id"),
                rs.getLong("holder_id"),
                AccountType.valueOf(rs.getString("type")),
                rs.getBigDecimal("balance")
        );
    }

    private RowMapper<Transaction> transactionRowMapper() {
        return (rs, rowNum) -> new Transaction(
                rs.getLong("id"),
                getNullableLong(rs, "sender_account_id"),
                getNullableLong(rs, "recipient_account_id"),
                rs.getBigDecimal("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("note")
        );
    }

    private RowMapper<Card> cardRowMapper() {
        return (rs, rowNum) -> new Card(
                rs.getLong("id"),
                rs.getLong("account_id"),
                CardType.valueOf(rs.getString("type")),
                rs.getBigDecimal("card_limit"),
                CardStatus.valueOf(rs.getString("status"))
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
