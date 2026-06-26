package com.example.airecruitment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(@NotBlank String title, @NotBlank String jdText) {
}
