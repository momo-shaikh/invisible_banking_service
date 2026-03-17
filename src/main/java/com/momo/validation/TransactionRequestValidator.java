package com.momo.validation;

import com.momo.dto.TransactionCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, TransactionCreateRequest> {
    @Override
    public boolean isValid(TransactionCreateRequest value, ConstraintValidatorContext context) {
        if (value == null || value.transactionType() == null) {
            return true;
        }
        return switch (value.transactionType()) {
            case DEPOSIT -> value.senderAccountId() == null && value.recipientAccountId() != null;
            case WITHDRAWAL -> value.senderAccountId() != null && value.recipientAccountId() == null;
            case TRANSFER -> value.senderAccountId() != null
                    && value.recipientAccountId() != null
                    && !value.senderAccountId().equals(value.recipientAccountId());
        };
    }
}
