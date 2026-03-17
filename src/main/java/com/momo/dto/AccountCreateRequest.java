package com.momo.dto;

import com.momo.model.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AccountCreateRequest(
        @PositiveOrZero long holderId,
        @NotNull AccountType accountType,
        @NotNull @PositiveOrZero BigDecimal balance
) {}
