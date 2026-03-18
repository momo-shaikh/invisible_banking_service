package com.momo.bootstrap;

import com.momo.model.Account;
import com.momo.model.AccountHolder;
import com.momo.model.AccountType;
import com.momo.model.CardStatus;
import com.momo.model.CardType;
import com.momo.store.JdbcStore;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements ApplicationRunner {
    private static final DemoUser[] DEMO_USERS = {
            new DemoUser(
                    "Ada Wong",
                    "ada@example.com",
                    "demo123",
                    new DemoAccount(AccountType.CHECKING, "1850.50", true),
                    new DemoAccount(AccountType.SAVINGS, "9200.00", false)
            ),
            new DemoUser(
                    "Ava Carter",
                    "ava@example.com",
                    "demo123",
                    new DemoAccount(AccountType.CHECKING, "640.25", true),
                    new DemoAccount(AccountType.SAVINGS, "2750.00", false)
            ),
            new DemoUser(
                    "Ben Ortiz",
                    "ben@example.com",
                    "demo123",
                    new DemoAccount(AccountType.CREDIT, "-240.00", true),
                    new DemoAccount(AccountType.CHECKING, "410.00", true)
            ),
            new DemoUser(
                    "Leon Kennedy",
                    "leon@example.com",
                    "demo123",
                    new DemoAccount(AccountType.CHECKING, "1325.75", true),
                    new DemoAccount(AccountType.SAVINGS, "15000.00", false)
            )
    };

    private final JdbcStore store;
    private final boolean enabled;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataSeeder(JdbcStore store, @Value("${app.demo-seed.enabled:true}") boolean enabled) {
        this.store = store;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        for (DemoUser user : DEMO_USERS) {
            AccountHolder holder = store.getHolderByEmail(user.email());
            if (holder == null) {
                holder = store.saveHolder(user.fullName(), user.email());
            }
            if (store.getPasswordHashByHolderId(holder.id()) == null) {
                store.saveCredentials(holder.id(), passwordEncoder.encode(user.password()));
            }
            seedAccountsIfMissing(holder, user.accounts());
        }
    }

    private void seedAccountsIfMissing(AccountHolder holder, DemoAccount[] accounts) {
        if (!store.getAccountsByHolder(holder.id()).isEmpty()) {
            return;
        }

        for (DemoAccount accountSeed : accounts) {
            Account account = store.saveAccount(
                    holder.id(),
                    accountSeed.accountType(),
                    new BigDecimal(accountSeed.balance())
            );
            if (accountSeed.createCard()) {
                store.saveCard(account.id(), toCardType(account.accountType()), cardLimitFor(account), CardStatus.ACTIVE);
            }
        }
    }

    private CardType toCardType(AccountType accountType) {
        return accountType == AccountType.CREDIT ? CardType.CREDIT : CardType.DEBIT;
    }

    private BigDecimal cardLimitFor(Account account) {
        return account.accountType() == AccountType.CREDIT
                ? BigDecimal.valueOf(10_000)
                : account.balance();
    }

    private record DemoUser(String fullName, String email, String password, DemoAccount... accounts) {
    }

    private record DemoAccount(AccountType accountType, String balance, boolean createCard) {
    }
}
