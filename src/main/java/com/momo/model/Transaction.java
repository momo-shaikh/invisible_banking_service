package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record Transaction(
        @Positive long id,
        @Positive Long senderAccountId,
        @Positive Long recipientAccountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType transactionType,
        String note
) {}
