package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record Card(
        @Positive long id,
        @Positive long accountId,
        @NotNull CardType type,
        @Positive BigDecimal cardLimit,
        @NotNull CardStatus status
) {}
