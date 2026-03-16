package com.momo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountHolderCreateRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email
) {}
