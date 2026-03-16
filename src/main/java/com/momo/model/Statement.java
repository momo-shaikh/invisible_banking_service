package com.momo.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record Statement(
        @Positive long id,
        @Positive long accountId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate
) {}
