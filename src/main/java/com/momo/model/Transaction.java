package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record Transaction(
        @Positive long id,
        @Positive long senderAccountId,
        @Positive long recipientAccountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType type,
        String note
) {}
