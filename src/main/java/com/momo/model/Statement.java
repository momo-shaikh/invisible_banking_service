package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Statement(
        @Positive long accountId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull BigDecimal currentBalance,
        @NotNull List<Transaction> transactions
) {}
