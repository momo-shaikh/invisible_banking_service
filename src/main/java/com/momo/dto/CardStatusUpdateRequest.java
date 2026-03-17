package com.momo.dto;

import com.momo.model.CardStatus;
import jakarta.validation.constraints.NotNull;

public record CardStatusUpdateRequest(@NotNull CardStatus status) {}
