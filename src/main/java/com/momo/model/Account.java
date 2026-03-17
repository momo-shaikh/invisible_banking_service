package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record Account(
        @Positive long id,
        @Positive long holderId,
        @NotNull AccountType accountType,
        @NotNull @PositiveOrZero BigDecimal balance
) {}
