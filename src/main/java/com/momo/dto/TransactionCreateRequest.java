package com.momo.dto;

import com.momo.model.TransactionType;
import com.momo.validation.ValidTransactionRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@ValidTransactionRequest
public record TransactionCreateRequest(
        @Positive Long senderAccountId,
        @Positive Long recipientAccountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType transactionType,
        String note
) {}
