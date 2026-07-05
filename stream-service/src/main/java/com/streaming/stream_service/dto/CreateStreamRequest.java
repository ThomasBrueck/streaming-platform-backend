package com.streaming.stream_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStreamRequest (

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title max 150 characters")
    String title,

    @Size(max = 500, message = "Description max 500 characters")
    String description,

    @Size(max = 100, message = "Category max 100 characters")
    String category
) {}
