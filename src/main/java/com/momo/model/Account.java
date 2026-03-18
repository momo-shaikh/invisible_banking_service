package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record Account(
        @Positive long id,
        @Positive long holderId,
        @NotNull AccountType accountType,
        @NotNull BigDecimal balance
) {}
