package com.momo.dto;

import com.momo.model.CardStatus;
import com.momo.model.CardType;
import jakarta.validation.constraints.NotNull;

public record CardCreateRequest(
        @NotNull CardType type,
        @NotNull CardStatus status
) {}
