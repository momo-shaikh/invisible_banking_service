package com.momo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AccountHolder(
        @Positive long id,
        @NotBlank String fullName,
        @NotBlank @Email String email
) {}
