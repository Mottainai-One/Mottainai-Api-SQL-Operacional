package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Firebase alone proves identity; SQL only resolves the existing staff assignment. */
public record FirebaseSessionRequest(@NotBlank @Size(max = 8192) String firebaseIdToken) { }
