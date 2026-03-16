package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Card(
        @Positive long id,
        @Positive long accountId,
        @NotNull CardStatus status
) {}
