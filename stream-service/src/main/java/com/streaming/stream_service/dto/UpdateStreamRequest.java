package com.streaming.stream_service.dto;

import com.streaming.stream_service.enums.StreamCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lets the owner edit their existing channel instead of creating a new one. */
public record UpdateStreamRequest (

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title max 150 characters")
    String title,

    @Size(max = 500, message = "Description max 500 characters")
    String description,

    StreamCategory category
) {}
